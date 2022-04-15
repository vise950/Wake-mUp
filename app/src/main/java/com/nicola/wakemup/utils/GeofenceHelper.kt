package com.nicola.wakemup.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.nicola.wakemup.service.GeofenceBroadcastReceiver

object GeofenceHelper {

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceIntent: Intent
    private lateinit var geofencePendingIntent: PendingIntent

    fun init(context: Context) {
        geofencingClient = LocationServices.getGeofencingClient(context)
        geofenceIntent = Intent(context, GeofenceBroadcastReceiver::class.java)
        geofencePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            geofenceIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(
        position: LatLng,
        radius: Float,
        onComplete: () -> Unit?,
        onError: (Exception) -> Unit?
    ) {
        geofencingClient.addGeofences(getGeofenceRequest(position, radius), geofencePendingIntent)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onError(it) }
    }

    fun removeGeofence(onComplete: () -> Unit?, onError: (Exception) -> Unit?) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onError(it) }
    }


    private fun getGeofenceRequest(position: LatLng, radius: Float): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(createGeofence(position, radius))
            .build()
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