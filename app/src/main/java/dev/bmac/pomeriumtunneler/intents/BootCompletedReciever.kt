package dev.bmac.pomeriumtunneler.intents

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bmac.pomeriumtunneler.service.TunnelService

class BootCompletedReciever: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        context.startForegroundService(Intent(context, TunnelService::class.java))
    }
}