package com.nicola.wakemup.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.nicola.wakemup.utils.NotificationHelper

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
//        GeofenceTransitionsJobIntentService.enqueueWork(context, intent)

        GeofencingEvent.fromIntent(intent)?.let {
            if (!it.hasError()) {
                if (it.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    NotificationHelper.show()
//                    this.sendBroadcast(AlarmHelper.getStartIntent())
                }
            } else {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(it.errorCode)
                Log.e("GeofenceBroadcastReceiver", errorMessage)
                return
            }
        }
    }
}