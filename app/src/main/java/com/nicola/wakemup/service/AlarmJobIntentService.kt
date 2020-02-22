package com.nicola.wakemup.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.nicola.wakemup.utils.AlarmHelper
import com.nicola.wakemup.utils.GEOFENCE_INTENT_SERVICE_JOB_ID
import com.nicola.wakemup.utils.START_ALARM
import com.nicola.wakemup.utils.STOP_ALARM

class AlarmJobIntentService : JobIntentService() {

    companion object {
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeofenceTransitionsJobIntentService::class.java, GEOFENCE_INTENT_SERVICE_JOB_ID, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        when (intent.action) {
            START_ALARM -> AlarmHelper.start()
            STOP_ALARM -> AlarmHelper.stop()
        }
    }
}