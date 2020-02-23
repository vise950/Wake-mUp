package com.nicola.wakemup.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.nicola.wakemup.service.GeofenceBroadcastReceiver

object GeofenceHelper {

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceIntent: Intent
    private lateinit var geofencePendingIntent: PendingIntent

    fun init(context: Context) {
        geofencingClient = LocationServices.getGeofencingClient(context)
        geofenceIntent = Intent(context, GeofenceBroadcastReceiver::class.java)
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, geofenceIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun addGeofence(position: LatLng, radius: Float) {
        geofencingClient.addGeofences(getGeofenceRequest(position, radius), geofencePendingIntent)
                .addOnSuccessListener {
                    "geofence added correctly".log()
                    //todo show ok message to user
                }
                .addOnFailureListener { it.log("error add geofence") }
    }

    fun removeGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    "geofence removed correctly".log()
                }
                .addOnFailureListener { it.log("error remove geofence") }
    }

    private fun getGeofenceRequest(position: LatLng, radius: Float): GeofencingRequest {
        return GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(createGeofence(position, radius))
                .build()
    }

    fun getGeofenceError(errorCode: Int): String =
            when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "GeoFence not available"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many GeoFences"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intents"
                else -> "Unknown error"
            }

    private fun createGeofence(position: LatLng, radius: Float): Geofence {
        return Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(position.latitude, position.longitude, radius)
                .setExpirationDuration(GEOFENCE_EXPIRE_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
    }
}