package com.nicola.wakemup.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import com.nicola.wakemup.utils.*


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { c ->
            intent?.let {
                when (it.action) {
                    START_ALARM -> {
                        c.startService(Intent(c, AlarmService::class.java))
                    }
                    STOP_ALARM -> {
                        if (Utils.isMyServiceRunning(c, AlarmService::class.java)) {
                            c.stopService(Intent(c, AlarmService::class.java))
//                            MainActivity().removeGeofence()
                        }

                        val notificationManager = c.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(NOTIFICATION_ID)
                    }
                    else -> {
                        "no action".error("ALARM RECEIVER")
                    }
                }
            }
        }
    }
}