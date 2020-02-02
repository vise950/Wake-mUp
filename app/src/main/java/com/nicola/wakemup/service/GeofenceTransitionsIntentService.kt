package com.nicola.wakemup.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.nicola.wakemup.R
import com.nicola.wakemup.activity.MainActivity
import com.nicola.wakemup.utils.*


class GeofenceTransitionsIntentService : IntentService(TAG) {

    companion object {
        private val TAG = "GeofenceTransitionsIntentService"
        private val CHANNEL_ID = "wakemup_notification"
    }

    private val notificationIntent by lazy { Intent(this, MainActivity::class.java) }
    private val stackBuilder by lazy {
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(notificationIntent)
        stackBuilder
    }
    private val notificationPendingIntent by lazy { stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT) }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notification by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_desc))
                .setWhen(Utils.getNowInMls())
                .setAutoCancel(true)
                .setOngoing(true)
                .setColor(Color.RED)
                .setColorized(true)
                .setVibrate(longArrayOf(0))
                .addAction(R.drawable.ic_alarm_off, getText(R.string.action_dismiss), intentStopAlarm)
                .setContentIntent(notificationPendingIntent)
    }

    private lateinit var channel: NotificationChannel

    private val intentStartAlarm by lazy {
        val startAlarm = Intent()
        startAlarm.action = START_ALARM
        startAlarm
    }

    private val intentStopAlarm by lazy {
        val stopAlarm = Intent(this, AlarmReceiver::class.java)
        stopAlarm.action = STOP_ALARM
        PendingIntent.getBroadcast(this, 2, stopAlarm, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onHandleIntent(intent: Intent?) {
        createNotificationChannel()
        GeofencingEvent.fromIntent(intent)?.let {
            if (!it.hasError()) {
                if (it.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    notificationManager.notify(NOTIFICATION_ID, notification.build())
                    sendBroadcastCompat(this, intentStartAlarm)
                }
            } else {
                getErrorString(it.errorCode).error()
            }
        }
    }

    private fun sendBroadcastCompat(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.sendBroadcast(intent)
            return
        }

        val broadcastIntent = Intent(intent)
        context.packageManager.queryBroadcastReceivers(broadcastIntent, 0).forEach {
            broadcastIntent.setClassName(it.activityInfo.packageName, it.activityInfo.name)
            context.sendBroadcast(broadcastIntent)
        }

    }

    //todo save error with analytics
    private fun getErrorString(errorCode: Int): String =
            when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "GeoFence not available"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many GeoFences"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intents"
                else -> "Unknown error"
            }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = NotificationChannel(CHANNEL_ID, "General", NotificationManager.IMPORTANCE_HIGH)
            channel.setBypassDnd(true)
            channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }
    }
}