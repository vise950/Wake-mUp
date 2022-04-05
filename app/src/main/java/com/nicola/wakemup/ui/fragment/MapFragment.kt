package com.nicola.wakemup.ui.fragment

import android.annotation.SuppressLint
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
import com.nicola.wakemup.databinding.FragmentMapBinding
import com.nicola.wakemup.utils.DEFAULT_ZOOM
import com.nicola.wakemup.utils.GeofenceHelper
import com.nicola.wakemup.utils.ZOOM
import com.nicola.wakemup.utils.locationUpdated

class MapFragment : Fragment(), OnMapReadyCallback {

    private var binding: FragmentMapBinding? = null

    private var googleMap: GoogleMap? = null
    private var location: Location? = null
    private var marker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        val view = binding!!.root

        val mapFragment =
            childFragmentManager.findFragmentById(binding!!.googleMap.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        handleView()

        locationUpdated = {
            location = it
            moveMapCamera(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        initMapView()
    }

    @SuppressLint("MissingPermission")
    private fun initMapView() {
        googleMap?.apply {
            isMyLocationEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isMapToolbarEnabled = false

            setOnMyLocationButtonClickListener {
                //fixme camera zoom animation
                location?.let { moveMapCamera(it) }
                true
            }

            setOnMapClickListener {
                removeMarker()
                addMarker(it)
            }
        }
    }

    private fun initView() {}

    private fun handleView() {
        binding!!.startGeofenceFab.setOnClickListener {
            marker?.let {
                GeofenceHelper.addGeofence(it.position, 300F)
            }
        }
    }

    private fun moveMapCamera(location: Location) {
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    location.latitude,
                    location.longitude
                ), DEFAULT_ZOOM
            )
        )
        googleMap?.animateCamera(CameraUpdateFactory.zoomTo(ZOOM), 2000, null)
    }

    private fun addMarker(position: LatLng) {
        marker = googleMap?.addMarker(MarkerOptions().position(position))
    }

    private fun removeMarker() {
        marker?.remove()
    }
}