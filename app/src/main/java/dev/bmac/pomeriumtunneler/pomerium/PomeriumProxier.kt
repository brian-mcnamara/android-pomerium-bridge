package dev.bmac.pomeriumtunneler.pomerium

import com.jetbrains.rd.util.lifetime.Lifetime
import com.salesforce.pomerium.AuthProvider
import com.salesforce.pomerium.CredentialKey
import com.salesforce.pomerium.CredentialStore
import com.salesforce.pomerium.PomeriumAuthProvider
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.joinTo
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpHeaders
import java.io.IOException
import java.net.URI
import kotlin.time.Duration.Companion.seconds


class PomeriumProxier(private val authProvider: AuthProvider, private val getAuthHost: suspend (route: URI) -> String) {


    suspend fun startProxy(route: URI, localPort: Int) = coroutineScope {
        val lifetime = Lifetime.Eternal.createNested()
        val selectorManager = SelectorManager(Dispatchers.IO + coroutineContext)
        val authHost = getAuthHost(route)
        try {
            val serverSocket = try {
                aSocket(selectorManager).tcp().bind("127.0.0.1", localPort)
            } catch (e: Exception) {
                log.error("Failed to create local socket on port $localPort", e)
                return@coroutineScope
            }
            lifetime.onTermination {
                serverSocket.close()
            }
            while (isActive) {
                val socket = try {
                    serverSocket.accept()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.debug("Failed to accept connection, continuing...")
                    continue
                }
                launch(Dispatchers.IO) {
                    try {
                        val token = withTimeout(10.seconds) {
                            authProvider.getAuth(route, lifetime).await()
                        }
                        val localReadChannel = socket.openReadChannel()
                        val localWriteChannel = socket.openWriteChannel(true)
                        val localIs = localReadChannel.toInputStream()
                        val localOs = localWriteChannel.toOutputStream()
                        val request = rawHttp.parseRequest(localIs).let {
                            return@let it.withHeaders(it.headers.and(RawHttpHeaders.newBuilder()
                                        .with("x-pomerium-authorization", token)
                                        .overwrite("Host", "${route.host}")
                                        .overwrite("Origin", "https://${route.host}")
                                        .with("X-Forwarded-For", "127.0.0.1")
                                        .build()
                                )
                            )
                        }


                        val (upgrade, close, keepAlive) = request.headers.getFirst("Connection")
                            .let {
                                listOf(
                                    it.isPresent && it.get().equals("upgrade", true),
                                    it.isPresent && it.get().equals("close", true),
                                    it.isPresent && it.get().equals("keep-alive", true)
                                )
                            }
                        val websocket = request.headers.getFirst("Upgrade").let {
                            upgrade && it.isPresent && it.get().equals("websocket", true)
                        }
                        val (keepaliveTimeout, maxConnections) = request.headers.getFirst("Keep-Alive")
                            .let { ka ->
                                val params = ka.orElse("")
                                    .split(",")
                                    .filter { it.contains("=") }
                                    .associate { it.split("=").first() to it.split("=")[1] }
                                listOf(params["timeout"]?.toLong(), params["max"]?.toLong())
                            }

                        //todo port
                        aSocket(selectorManager).tcp().connect(route.host, 443) {
                            this.keepAlive = keepAlive
                            this.socketTimeout = keepaliveTimeout ?: Long.MAX_VALUE
                        }.tls(coroutineContext) {
                            serverName = route.host
                        }.use {
                            val remoteReadChannel = it.openReadChannel()
                            val remoteWriteChannel = it.openWriteChannel(true)
                            val remoteOs = remoteWriteChannel.toOutputStream()
                            val remoteIs = remoteReadChannel.toInputStream()

                            request.writeTo(remoteOs)

                            val response = rawHttp.parseResponse(remoteIs)
                            if (response.statusCode == 302) {
                                val location = response.headers.getFirst("Location")
                                if (location.isPresent) {
                                    val uri = URI.create(location.get())
                                    if (uri.host == authHost) {
                                        authProvider.invalidate(route)
                                        val redirect = response.withHeaders(
                                            response.headers
                                                .and(
                                                    RawHttpHeaders.newBuilder()
                                                        .overwrite(
                                                            "Location",
                                                            "http://localhost:$localPort${request.startLine.uri.path}"
                                                        )
                                                        .overwrite("Connection", "close")
                                                        .build()
                                                )
                                        )
                                        redirect.writeTo(localOs)
                                        return@use
                                    }
                                }
                            }

                            response.writeTo(localOs)
                            if (!close) {
                                val connection = response.headers.getFirst("Connection")
                                if (connection.isPresent && connection.get()
                                        .equals("close", true)
                                ) {
                                    return@use
                                }
                                if (response.statusCode == 101) {
                                    if (websocket) {
                                        launch(Dispatchers.IO) {
                                            try {
                                                localReadChannel.joinTo(
                                                    remoteWriteChannel,
                                                    true
                                                )
                                            } catch (e: IOException) {
                                                socket.close();
                                            }
                                        }
                                        remoteReadChannel.joinTo(localWriteChannel, true)
                                    } else {
                                        launch(Dispatchers.IO) {
                                            try {
                                                remoteReadChannel.joinTo(
                                                    localWriteChannel,
                                                    true
                                                )
                                            } catch (e: IOException) {
                                                socket.close()
                                            }
                                        }
                                        do {
                                            localReadChannel.awaitContent()
                                            if (localReadChannel.isClosedForRead) {
                                                break
                                            }
                                            val request = try {
                                                rawHttp.parseRequest(localIs)
                                            } catch (e: Exception) {
                                                log.error("Exception occurred during keep-alive connection", e)
                                                null
                                            }?.let {
                                                return@let it.withHeaders(
                                                    it.headers
                                                        .and(
                                                            RawHttpHeaders.newBuilder().with(
                                                                "x-pomerium-authorization",
                                                                token
                                                            ).overwrite(
                                                                "Host",
                                                                "${route.host}:443"
                                                            )
                                                                .with(
                                                                    "X-Forwarded-For",
                                                                    "127.0.0.1"
                                                                )
                                                                .build()
                                                        )
                                                )
                                            }
                                            request?.writeTo(remoteOs)
                                        } while (keepAlive)
                                    }
                                } else {
                                    log.debug("Server was unable to upgrade connection")
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        //Do nothing for this case
                    } catch (e: Exception) {
                        log.error("Proxing request failed with exception", e)
                    } finally {
                        withContext(NonCancellable) {
                            socket.close()
                        }
                    }
                }
            }
        } finally {
            lifetime.terminate()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PomeriumProxier::class.java)
        private val rawHttp = RawHttp()
    }
}

fun main(args: Array<String>) {
    runBlocking {
        val authProvider = PomeriumAuthProvider(object : CredentialStore {
            val store = HashMap<CredentialKey, String>()
            override fun getToken(key: CredentialKey): String? {
                return store[key]
            }

            override fun setToken(key: CredentialKey, jwt: String) {
                store[key] = jwt
            }

            override fun clearToken(key: CredentialKey) {
                store.remove(key)
            }

        })
        PomeriumProxier(authProvider) { authProvider.getAuthHost(it) }.startProxy(URI.create(args[1]), 6789)
    }
}