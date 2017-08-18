package com.nicola.wakemup.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.PowerManager
import com.nicola.wakemup.utils.Constant
import com.nicola.wakemup.utils.Utils
import com.nicola.wakemup.utils.error


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val pm = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wl = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "")
        wl?.acquire()

        context?.let { c ->
            intent?.let {
                when (it.action) {
                    Constant.START_ALARM -> {
                        c.startService(Intent(c, AlarmService::class.java))
                    }
                    Constant.STOP_ALARM -> {
                        if (Utils.isMyServiceRunning(c, AlarmService::class.java)) {
                            c.stopService(Intent(c, AlarmService::class.java))
                        }

                        val notificationManager = c.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(Constant.NOTIFICATION_ID)

                        wl?.release()
                    }
                    else -> {
                        "no action".error("ALARM RECEIVER")
                    }
                }
            }
        }
    }
}