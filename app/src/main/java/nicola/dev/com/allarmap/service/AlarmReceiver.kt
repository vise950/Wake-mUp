package nicola.dev.com.allarmap.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "STOP_ALARM") {
            val service = GeofenceTransitionsIntentService()
//            service.stopService(Intent(context, GeofenceTransitionsIntentService::class.java))
            service.stopSelf()

//            context.stopService(Intent(context,GeofenceTransitionsIntentService::class.java))

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(999)
        }
    }

}