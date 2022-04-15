package com.nicola.wakemup.ui.fragment

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.nicola.wakemup.R
import com.nicola.wakemup.databinding.FragmentMapBinding
import com.nicola.wakemup.utils.GeofenceHelper
import com.nicola.wakemup.utils.hasPermission
import com.nicola.wakemup.utils.locationUpdated
import com.nicola.wakemup.utils.showSnackbar
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private val TAG = "ALARM MAP"
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private val DEFAULT_ZOOM: Float = 0F
        private val ZOOM: Float = 8F
        private val GEOFENCE_REQ_ID = "Alarm map geofence"
        private val GEO_DURATION = 60 * 60 * 1000L
    }


    private var binding: FragmentMapBinding? = null

    private var googleMap: GoogleMap? = null
    private var location: Location? = null
    private var marker: Marker? = null
    private var circle: Circle? = null
    private val circleOptions by lazy {
        CircleOptions().strokeWidth(10F)
            .strokeColor(ContextCompat.getColor(requireContext(), R.color.stoke_map))
            .fillColor(ContextCompat.getColor(requireContext(), R.color.fill_map))
    }

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
        if (requireContext().hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            googleMap?.isMyLocationEnabled = true
        }

        googleMap?.apply {
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isMapToolbarEnabled = false

            setOnMyLocationButtonClickListener {
                //fixme camera zoom animation
                location?.let { moveMapCamera(it) }
                true
            }

            setOnMapClickListener {
                removeCircle()
                binding!!.radiusSeekbar.progress = 0

                removeMarker()
                addMarker(it)
            }
        }
    }

    private fun initView() {}

    private fun handleView() {
        binding!!.radiusSeekbar.setOnProgressChangeListener(object :
            DiscreteSeekBar.OnProgressChangeListener {
            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onProgressChanged(
                seekBar: DiscreteSeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                marker?.let {
                    removeCircle()
                    addCircle(it, progress)
                }
            }
        })

        binding!!.startGeofenceFab.setOnClickListener {
            marker?.let {
                GeofenceHelper.addGeofence(it.position, circleOptions.radius.toFloat(),
                    onComplete = {
                        Log.e("GEOFENCE", "geofence added successfully")
                        binding!!.root.showSnackbar(
                            "geofence added successfully",
                            Snackbar.LENGTH_SHORT,
                            "OK"
                        )
                    },
                    onError = {
                        Log.e("GEOFENCE", "error during create geofence", it.fillInStackTrace())
                        binding!!.root.showSnackbar(
                            "error during create geofence",
                            Snackbar.LENGTH_SHORT,
                            "Retry"
                        ) {
                            //todo clean all geofence and retry
                        }

                    })
            }.run {
                binding!!.root.showSnackbar("no marker", Snackbar.LENGTH_LONG, "OK")
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

    private fun addCircle(marker: Marker, progress: Int) {
        circleOptions.center(marker.position)
        circleOptions.radius(progress * 1000.0)
        circle = googleMap?.addCircle(circleOptions)
    }

    private fun removeCircle() {
        circle?.remove()
        circleOptions.radius(0.0)
    }
}