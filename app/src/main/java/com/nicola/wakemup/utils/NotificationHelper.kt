package com.nicola.wakemup.utils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.nicola.wakemup.R
import com.nicola.wakemup.ui.activity.MainActivity

object NotificationHelper {

    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: Notification

    fun init(context: Context) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        buildNotification(context)
    }

    fun show() {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun buildNotification(context: Context) {
        notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(context.getText(R.string.notification_title))
                .setContentText(context.getText(R.string.notification_desc))
                .setWhen(Utils.getNowInMls())
                .setAutoCancel(true)
                .setOngoing(true)
                .setColor(Color.RED)
                .setColorized(true)
                .addAction(R.drawable.ic_alarm_off, context.getText(R.string.action_dismiss), AlarmHelper.getStopPendingIntent())
                .setContentIntent(createNotificationIntent(context))
                .build()
    }

    //todo evitare di ricreare il channel ogni volta che richiamo la classe
    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        NotificationChannel(NOTIFICATION_CHANNEL_ID, "General", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    setBypassDnd(true)
                    enableVibration(false)
                }.also {
                    notificationManager.createNotificationChannel(it)
                }
    }

    private fun createNotificationIntent(context: Context): PendingIntent? {
        with(Intent(context, MainActivity::class.java)) {
            TaskStackBuilder.create(context).apply {
                addParentStack(MainActivity::class.java)
                addNextIntent(this@with)
            }
        }.also {
            return it.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}