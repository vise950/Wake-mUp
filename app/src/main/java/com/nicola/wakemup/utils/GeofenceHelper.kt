package com.nicola.wakemup.utils

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng

object GeofenceHelper {

    fun getGeofenceRequest(position: LatLng, radius: Float): GeofencingRequest {
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