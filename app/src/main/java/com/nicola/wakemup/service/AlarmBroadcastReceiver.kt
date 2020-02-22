package com.nicola.wakemup.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nicola.wakemup.utils.NotificationHelper


class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AlarmJobIntentService.enqueueWork(context, intent)
        NotificationHelper.dismiss()
    }
}