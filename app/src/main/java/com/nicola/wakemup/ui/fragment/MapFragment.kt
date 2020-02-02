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
import com.nicola.wakemup.R
import com.nicola.wakemup.utils.locationUpdated
import com.nicola.wakemup.utils.log


class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    companion object {
        private val DEFAULT_ZOOM: Float = 0F
        private val ZOOM: Float = 8F
    }

    private var map: GoogleMap? = null
    private var location: Location? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_map, null)

        (childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment?)
                ?.getMapAsync(this)

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationUpdated = {
            "location updated".log()
            "lat: ${it.latitude}, lng: ${it.longitude}".log()
            location = it
            updateMapUi()
        }

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let { pennywise ->
            this.map = pennywise
            pennywise.setOnMapLongClickListener(this)
        }
    }

    override fun onMapLongClick(latLng: LatLng?) {
//        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
//        latLng?.let {
//            clearMap(it)
//
//            Intent(this, FetchAddressIntentService::class.java).apply {
//                putExtra(RECEIVER, addressReceiver)
//                putExtra(LOCATION_DATA_EXTRA, it)
//            }.let {
//                startService(it)
//            }
//
//            addressReceiver.onResultReceive = {
//                runOnUiThread {
//                    place_autocomplete_tv.setText(it)
//                    place_autocomplete_tv.dismissDropDown()
//                }
//            }
//        }
    }

    private fun updateMapUi() {
        map?.apply {
            isMyLocationEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isMapToolbarEnabled = false
        }

        map?.setOnMyLocationButtonClickListener {
            location?.let { map?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))) }
            true
        }

        location?.let {
            //            if (isAppRunning == false) {
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM))
            map?.animateCamera(CameraUpdateFactory.zoomTo(ZOOM), 2000, null)
//                isAppRunning = true
//        }
        }
    }
}