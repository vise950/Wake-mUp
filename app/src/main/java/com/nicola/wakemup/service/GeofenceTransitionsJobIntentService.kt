package com.nicola.wakemup.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.nicola.wakemup.utils.GEOFENCE_INTENT_SERVICE_JOB_ID
import com.nicola.wakemup.utils.GeofenceHelper
import com.nicola.wakemup.utils.NotificationHelper
import com.nicola.wakemup.utils.log


class GeofenceTransitionsJobIntentService : JobIntentService() {

    companion object {
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeofenceTransitionsJobIntentService::class.java, GEOFENCE_INTENT_SERVICE_JOB_ID, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.init(this)
    }

    override fun onHandleWork(intent: Intent) {
        GeofencingEvent.fromIntent(intent)?.let {
            if (!it.hasError()) {
                if (it.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    NotificationHelper.showNotification()
//                    sendBroadcastCompat(this, intentStartAlarm)
                    "enter in geofence"
                }
            } else {
                GeofenceHelper.getGeofenceError(it.errorCode).log()
            }
        }
    }
}