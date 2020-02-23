package com.nicola.wakemup.ui.fragment

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.nicola.wakemup.R
import com.nicola.wakemup.utils.DEFAULT_ZOOM
import com.nicola.wakemup.utils.GeofenceHelper
import com.nicola.wakemup.utils.ZOOM
import com.nicola.wakemup.utils.locationUpdated
import kotlinx.android.synthetic.main.fragment_map.*


class MapFragment : Fragment(), OnMapReadyCallback {

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

        initView()

        locationUpdated = {
            location = it
            updateMapUi()
        }

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let { pennywise ->
            this.map = pennywise
        }
    }

    private fun initView(){
        handleViewListener()
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

    private fun handleViewListener(){
        start_geofence_fab.setOnClickListener {
            marker?.let {
                GeofenceHelper.addGeofence(it.position, 300F)
            }
        }
    }
}