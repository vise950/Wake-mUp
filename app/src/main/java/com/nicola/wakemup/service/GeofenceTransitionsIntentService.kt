package com.nicola.wakemup.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.nicola.wakemup.R
import com.nicola.wakemup.activity.MainActivity
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
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.ic_alarm_off, getText(R.string.action_dismiss), mIntentStopAlarm)
                .setContentIntent(mNotificationPendingIntent)
    }
    private val CHANNEL_ID = "wakemup_notification"
    private lateinit var mChannel: NotificationChannel


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
        createNotificationChannel()
        GeofencingEvent.fromIntent(intent)?.let {
            if (!it.hasError()) {
                if (it.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    mNotificationManager.notify(Constant.NOTIFICATION_ID, mNotification.build())
                    sendBroadcastCompat(this, mIntentStartAlarm)
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
            mChannel = NotificationChannel(CHANNEL_ID, "General", NotificationManager.IMPORTANCE_HIGH)
            mChannel.setBypassDnd(true)
            mChannel.enableVibration(false)
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }
}