package dev.bmac.pomeriumtunneler.pomerium

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jetbrains.rd.framework.util.launch
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.salesforce.pomerium.AuthLinkHandler
import dev.bmac.pomeriumtunneler.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.URI
import kotlin.time.Duration.Companion.minutes

class AndroidAuthLinkHandler(private val applicationContext: Context): AuthLinkHandler {
    override fun handleAuthLink(
        getLink: () -> URI,
        jobLifetime: LifetimeDefinition,
        newRoute: Boolean
    ) {
        jobLifetime.launch {
            while(isActive) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getLink().toString())).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder =
                    NotificationCompat.Builder(applicationContext, "dev.bmac.pomerium").apply {
                        setSmallIcon(R.drawable.q)
                        priority = NotificationCompat.PRIORITY_HIGH
                        addAction(R.drawable.q, "Authenticate", pendingIntent)
                        setContentTitle("Authentication required for tunneler")
                        setOnlyAlertOnce(true)
                        setOngoing(true)
                    }

                with(NotificationManagerCompat.from(applicationContext)) {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return@with
                    }
                    notify(0, builder.build())
                    jobLifetime.onTermination {
                        cancel(0)
                    }
                }
                delay(1.minutes)
            }
        }
    }

    companion object {
        const val ID = "ID"
    }
}