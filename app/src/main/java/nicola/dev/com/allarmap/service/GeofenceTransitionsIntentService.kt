package nicola.dev.com.allarmap.service

import android.app.IntentService
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import nicola.dev.com.allarmap.R
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.support.v7.app.NotificationCompat
import nicola.dev.com.allarmap.ui.activity.MainActivity
import com.dev.nicola.allweather.utils.log
import com.google.android.gms.location.GeofenceStatusCodes
import nicola.dev.com.allarmap.utils.Utils


class GeofenceTransitionsIntentService : IntentService("") {

    companion object {
        private val TAG = "GeofenceTransitionsIntentService"
        private val GEOFENCE_NOTIFICATION_ID = 0
    }


    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            getErrorString(geofencingEvent.errorCode).log(TAG)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            createNotification()
            "notify".log(TAG)
        } else {
            // Log the error.
            "transition invalid".log(TAG)
        }
    }

//    // Send a notification
//    private fun sendNotification(msg: String) {
//        "sendNotification: $msg".log(TAG)
//
//
//        // Intent to start the main Activity
//        val notificationIntent = MainActivity.makeNotificationIntent(this, msg)
//
//        val stackBuilder = TaskStackBuilder.create(this)
//        stackBuilder.addParentStack(MainActivity::class.java)
//        stackBuilder.addNextIntent(notificationIntent)
//        val notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
//
//        // Creating and sending Notification
//        val notificatioMng = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificatioMng.notify(GEOFENCE_NOTIFICATION_ID,
//                createNotification(msg, notificationPendingIntent))
//    }

    //    // Create a notification
//    private fun createNotification() {
//        val notification = NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher_round)
//                .setContentTitle("Title")
//                .setContentText("Geofence Notification!")
//                .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND)
//                .build()
//
//
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT)
//        notification.setContentIntent(contentIntent)
//
//        // Add as notification
//        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        manager.notify(0, builder.build())
//    }


    private fun createNotification() {
        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Notifications Example")
                .setContentText("This is a test notification")
                .setWhen(Utils.getNowInMls())
                .setAutoCancel(true)
//                .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, GEOFENCE_NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(contentIntent)

        // Add as notification
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
    }


    // Handle errors
    private fun getErrorString(errorCode: Int): String {
        when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> return "GeoFence not available"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> return "Too many GeoFences"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> return "Too many pending intents"
            else -> return "Unknown error."
        }
    }
}