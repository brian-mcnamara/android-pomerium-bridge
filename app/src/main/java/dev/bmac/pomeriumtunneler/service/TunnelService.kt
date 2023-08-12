package dev.bmac.pomeriumtunneler.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.asLiveData
import com.salesforce.pomerium.PomeriumAuthProvider
import dev.bmac.pomeriumtunneler.R
import dev.bmac.pomeriumtunneler.pomerium.AndroidAuthLinkHandler
import dev.bmac.pomeriumtunneler.pomerium.AndroidCredentialStore
import dev.bmac.pomeriumtunneler.pomerium.PomeriumProxier
import dev.bmac.pomeriumtunneler.storage.AppDatabase
import dev.bmac.pomeriumtunneler.storage.RouteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.net.URI

class TunnelService : Service() {

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val repository by lazy { RouteRepository(db.routesDao()) }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val authLinkHandler by lazy { AndroidAuthLinkHandler(applicationContext) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        (applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
            createNotificationChannel(NotificationChannel("dev.bmac.pomerium", "Authenticate", NotificationManager.IMPORTANCE_HIGH))
        }
        startForeground(10, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        val authService = PomeriumAuthProvider(
            AndroidCredentialStore(applicationContext),
            authLinkHandler
        )

        repository.routes.asLiveData().observeForever { routes ->
            val ids = routes.map { it.id }.toSet()
            jobMap.filterKeys { !ids.contains(it) }.forEach {
                it.value.cancel()
                jobMap.remove(it.key)
            }
            routes.forEach {
                jobMap[it.id]?.cancel()
                jobMap[it.id] = scope.async {
                    try {
                        PomeriumProxier(authService) { authService.getAuthHost(it) }.startProxy(
                            URI.create(it.route),
                            it.localPort
                        )
                    } catch (e: Exception) {
                        Log.e("tunnelService", "", e)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(): Notification {
        (applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).also {
            it.createNotificationChannel(NotificationChannel("dev.bmac.pomerium.tunneler", "Tunneling", NotificationManager.IMPORTANCE_NONE))
        }
        return NotificationCompat.Builder(this, "dev.bmac.pomerium.tunneler")
            .setContentTitle("Tunneling in progress")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSmallIcon(R.drawable.q)
            .build()
    }


    companion object {
        private val jobMap = HashMap<Int, Job>()
    }
}