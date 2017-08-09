package com.nicola.wakemup.service

import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.nicola.wakemup.R
import com.nicola.wakemup.ui.activity.MainActivity
import com.nicola.wakemup.utils.Constant
import com.nicola.wakemup.utils.Utils
import com.nicola.wakemup.utils.error

@Suppress("DEPRECATION")
class GeofenceTransitionsIntentService : IntentService(TAG) {

    companion object {
        private val TAG = "GeofenceTransitionsIntentService"
    }

    private val mNotificationIntent by lazy { Intent(this, MainActivity::class.java) }
    private val mStackBuilder by lazy {
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(mNotificationIntent)
        stackBuilder
    }
    private val mNotificationPendingIntent by lazy { mStackBuilder?.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT) }
    private val mNotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val mNotification by lazy {
        NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("Hey! Wake up")
                .setContentText("You arrived to destination")
                .setWhen(Utils.getNowInMls())
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.ic_alarm_off, "Dismiss", mIntentStopAlarm)
                .setContentIntent(mNotificationPendingIntent)
        //todo string
    }

    private val mIntentStartAlarm by lazy {
        val startAlarm = Intent()
        startAlarm.action = Constant.START_ALARM
        startAlarm
    }

    private val mIntentStopAlarm by lazy {
        val stopAlarm = Intent()
        stopAlarm.action = Constant.STOP_ALARM
        PendingIntent.getBroadcast(this, 2, stopAlarm, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onHandleIntent(intent: Intent?) {
        GeofencingEvent.fromIntent(intent)?.let {
            if (!it.hasError()) {
                if (it.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    mNotificationManager.notify(Constant.NOTIFICATION_ID, mNotification.build())
                    sendBroadcast(mIntentStartAlarm)
                }
            } else {
                getErrorString(it.errorCode).error()
            }
        }
    }

    // Handle errors
    private fun getErrorString(errorCode: Int): String {
        when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> return "GeoFence not available"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> return "Too many GeoFences"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> return "Too many pending intents"
            else -> return "Unknown error"
        }
    }
}