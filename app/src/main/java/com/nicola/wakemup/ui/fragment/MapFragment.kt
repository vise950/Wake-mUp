package com.nicola.wakemup.ui.fragment

import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.nicola.wakemup.R
import com.nicola.wakemup.service.GeofenceBroadcastReceiver
import com.nicola.wakemup.utils.GeofenceHelper
import com.nicola.wakemup.utils.locationUpdated
import com.nicola.wakemup.utils.log


class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val DEFAULT_ZOOM: Float = 0F
        private const val ZOOM: Float = 10F
    }

    private val geofencingClient by lazy { LocationServices.getGeofencingClient(this.requireActivity()) }
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this.requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this.requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private var map: GoogleMap? = null
    private var location: Location? = null
    private var marker: Marker? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_map, null)

        (childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment?)
                ?.getMapAsync(this)

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationUpdated = {
            "location -- lat: ${it.latitude}, lng: ${it.longitude}".log()
            location = it
            updateMapUi()
        }

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let { pennywise ->
            this.map = pennywise
        }
    }

    private fun updateMapUi() {
        map?.apply {
            isMyLocationEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isMapToolbarEnabled = false

            setOnMyLocationButtonClickListener {
                //fixme camera zoom animation
                location?.let { moveMapCamera(it) }
                true
            }

            //todo può essere settato un solo marker
            // se l'utente clicca più volte sulla mappa per aggiundgere altri marker va avvisato con una snackbar o toast che deve prima rimuovere quello già presente
            // possibilità di modificare il raggio di azione del marker o eliminarlo al click
            setOnMapClickListener {
                addMarker(it)
                addGeofence(it, 200F)
            }
        }

        location?.let { moveMapCamera(it) }
    }

    private fun moveMapCamera(location: Location) {
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))
        map?.animateCamera(CameraUpdateFactory.zoomTo(ZOOM), 2000, null)
    }

    private fun addMarker(position: LatLng) {
        marker = map?.addMarker(MarkerOptions()
                .position(position))
    }

    private fun removeMarker() {
        marker?.remove()
    }

    private fun addGeofence(position: LatLng, radius: Float) {
        geofencingClient.addGeofences(GeofenceHelper.getGeofenceRequest(position, radius), geofencePendingIntent)
                .addOnSuccessListener {
                    "geofence added correctly".log()
                    //todo show ok message to user
                }
                .addOnFailureListener { it.log("error add geofence") }
    }

    private fun removeGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    "geofence removed correctly".log()
                }
                .addOnFailureListener { it.log("error remove geofence") }
    }
}