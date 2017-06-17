package nicola.dev.com.allarmap.ui.activity

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.seatgeek.placesautocomplete.model.AutocompleteResultType
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_details.*
import nicola.dev.com.allarmap.R
import nicola.dev.com.allarmap.service.AlarmService
import nicola.dev.com.allarmap.service.GeofenceTransitionsIntentService
import nicola.dev.com.allarmap.utils.PreferencesHelper
import nicola.dev.com.allarmap.utils.Utils
import nicola.dev.com.allarmap.utils.log
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar
import java.util.*


class MainActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    companion object {
        private val TAG = "ALLARM MAP"
        private val REQUEST_LOCATION = 854
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private val DEFAULT_ZOOM: Float = 6F
        private val ZOOM: Float = 8F
        private val GEOFENCE_REQ_ID = "Allarm map geofence"
        private val GEO_DURATION = 60 * 60 * 1000L


        private val INVALID_FLOAT_VALUE = -999.0F
        private val INVALID_INT_VALUE = -999
        private val INVALID_DOUBLE_VALUE = -999.0
        private val INVALID_STRING_VALUE = ""
    }

    private var mMap: GoogleMap? = null
    private var mCircle: Circle? = null
    private var mRadius: Double? = null
    private var mRequestPermissionCount = 0

    private val mGoogleApiClient by lazy <GoogleApiClient> {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build()
    }

    private val mLocationRequest by lazy {
        LocationRequest()
    }

    private var mLocation: Location? = null
    private var mMarker: Marker? = null

    private val mGeofence by lazy <Geofence> {
        Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(mMarker?.position?.latitude ?: INVALID_DOUBLE_VALUE, mMarker?.position?.longitude ?: INVALID_DOUBLE_VALUE, mRadius?.toFloat() ?: INVALID_FLOAT_VALUE)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
    }

    private val mGeofenceRequest by lazy <GeofencingRequest> {
        GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(mGeofence)
                .build()
    }

    private val mGeoFencePendingIntent by lazy<PendingIntent> {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val mGeofenceClient by lazy { LocationServices.getGeofencingClient(this) }

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    private var isBusSelected = true
    private var isTrainSelected = false
    private var isPlaneSelected = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initMap()
        initUi()
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient.connect()

    }

    override fun onResume() {
        super.onResume()
        mGoogleApiClient.reconnect()

        if (Utils.isMyServiceRunning(this, AlarmService::class.java)) {
            stopService(Intent(this, AlarmService::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (mGoogleApiClient.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
            mGoogleApiClient.disconnect()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            this.mMap = it
            it.setOnMapClickListener(this)
        }
    }

    override fun onMapClick(latLng: LatLng?) {
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        mMarker?.remove()
        mCircle?.remove()
        latLng?.let {
            mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)))
            mMap?.animateCamera(CameraUpdateFactory.newLatLng(it))
            Utils.LocationHelper.getLocationName(it.latitude, it.longitude, {
                it.log("click map")
                destination_txt.setText(it)
                destination_txt.setCompletionEnabled(false)
                //fixme se creo un marker in un paese africano il nome non cambia
            })
        }
    }

    override fun onConnected(bundle: Bundle?) {
        if (Utils.isPermissionGranted(this)) {
            getDeviceLocation()
        } else {
            requestLocationPermission()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        mGeoFencePendingIntent.let {
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, it)
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {}

    override fun onLocationChanged(location: Location?) {
        this.mLocation = location
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getDeviceLocation()
                } else {
                    if (mRequestPermissionCount < 2 && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Utils.AlertHepler.snackbar(this, R.string.snackbar_ask_permission, actionClick = {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
                        })
                        mRequestPermissionCount++
                    } else {
                        Utils.AlertHepler.snackbar(this, R.string.snackbar_permission_denied, duration = Snackbar.LENGTH_LONG)
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    private fun setMapUi(map: GoogleMap) {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true

        mLocation?.let {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM))
            map.animateCamera(CameraUpdateFactory.zoomTo(ZOOM), 1000, null)
        }
    }

    private fun initUi() {
        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        mBottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.color_white))
                        destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_dark))
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary))
                        destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_light))
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) {
                    destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary))
                    destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_light))
                } else {
                    destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.color_white))
                    destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_dark))
                }
            }
        })

        bus_btn.setOnClickListener {
            if (isTrainSelected || isPlaneSelected) {
                train_btn.background = getDrawable(R.drawable.btn_background_unselected)
                train_btn.setImageDrawable(getDrawable(R.drawable.ic_train_black))
                plane_btn.background = getDrawable(R.drawable.btn_background_unselected)
                plane_btn.setImageDrawable(getDrawable(R.drawable.ic_plane_black))
                isTrainSelected = false
                isPlaneSelected = false
            }
            if (!isBusSelected) {
                bus_btn.background = getDrawable(R.drawable.btn_background_selected)
                bus_btn.setImageDrawable(getDrawable(R.drawable.ic_bus_white))
                isBusSelected = true
                radius_seekbar.min = 1
                radius_seekbar.max=50
                radius_seekbar.progress = radius_seekbar.min
            }
        }

        train_btn.setOnClickListener {
            if (isBusSelected || isPlaneSelected) {
                bus_btn.background = getDrawable(R.drawable.btn_background_unselected)
                bus_btn.setImageDrawable(getDrawable(R.drawable.ic_bus_black))
                plane_btn.background = getDrawable(R.drawable.btn_background_unselected)
                plane_btn.setImageDrawable(getDrawable(R.drawable.ic_plane_black))
                isBusSelected = false
                isPlaneSelected = false
            }
            if (!isTrainSelected) {
                train_btn.background = getDrawable(R.drawable.btn_background_selected)
                train_btn.setImageDrawable(getDrawable(R.drawable.ic_train_white))
                isTrainSelected = true
                radius_seekbar.min = 1
                radius_seekbar.max=200
                radius_seekbar.progress = radius_seekbar.min
            }
        }

        plane_btn.setOnClickListener {
            if (isBusSelected || isTrainSelected) {
                bus_btn.background = getDrawable(R.drawable.btn_background_unselected)
                bus_btn.setImageDrawable(getDrawable(R.drawable.ic_bus_black))
                train_btn.background = getDrawable(R.drawable.btn_background_unselected)
                train_btn.setImageDrawable(getDrawable(R.drawable.ic_train_black))
                isBusSelected = false
                isTrainSelected = false
            }
            if (!isPlaneSelected) {
                plane_btn.background = getDrawable(R.drawable.btn_background_selected)
                plane_btn.setImageDrawable(getDrawable(R.drawable.ic_plane_white))
                isPlaneSelected = true
                radius_seekbar.min = 100
                radius_seekbar.max = 500
                radius_seekbar.progress = radius_seekbar.min
            }
        }

        radius_seekbar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener{
            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onProgressChanged(seekBar: DiscreteSeekBar?, progress: Int, fromUser: Boolean) {
                mRadius = 1000.0 * progress
                val circleOptions = CircleOptions()
                        .center(LatLng(mMarker?.position?.latitude ?: INVALID_DOUBLE_VALUE, mMarker?.position?.longitude ?: INVALID_DOUBLE_VALUE))
                        .radius(mRadius ?: INVALID_DOUBLE_VALUE)
                        .strokeWidth(10F)
                        .strokeColor(ContextCompat.getColor(this@MainActivity, R.color.stoke_map))
                        .fillColor(ContextCompat.getColor(this@MainActivity, R.color.fill_map))

                mCircle?.remove()
                mCircle = mMap?.addCircle(circleOptions)
                //todo improve code
            }
        })

        destination_txt.languageCode = Locale.getDefault().language
        destination_txt.resultType = AutocompleteResultType.GEOCODE
        destination_txt.historyManager = null
        destination_txt.setOnPlaceSelectedListener {
            Utils.hideKeyboard(this)
            Utils.LocationHelper.getCoordinates(it.description, {
                mMarker?.remove()
                mCircle?.remove()
                mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)))
                mMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude,it.longitude)))
            })
        }

        destination_txt.setOnClickListener {
            destination_txt.setCompletionEnabled(true)
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            if (destination_txt?.text?.isNotEmpty() ?: false) {
                destination_txt.text = null
            }
        }

        alarm_check.setOnCheckedChangeListener { compoundButton, checked ->
            PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, checked)
        }

        fab.setOnClickListener {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            if (mMarker != null && destination_txt.text.isNotEmpty()) {
                mLocation?.let {
                    if (PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_PREF_GEOFENCE, false) as Boolean) {
                        Utils.AlertHepler.dialog(this, R.string.dialog_title_another_service, R.string.dialog_message_another_service, {
                            removeGeofence({
                                addGeofence()
                            })
                        })
                    } else {
                        addGeofence()
                    }
                }
            } else {
                Utils.AlertHepler.snackbar(this, R.string.snackbar_no_location, duration = Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun addGeofence() {
        mGeofenceClient.addGeofences(mGeofenceRequest, mGeoFencePendingIntent)
                .addOnSuccessListener {
                    Utils.AlertHepler.snackbar(this, R.string.snackbar_service_start, actionClick = { finishAndRemoveTask() })
                    PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_PREF_GEOFENCE, true)
                }
                .addOnFailureListener { it.log(TAG) }
    }

    private fun removeGeofence(callback: (() -> Unit)) {
        mGeofenceClient.removeGeofences(mGeoFencePendingIntent)
                .addOnSuccessListener { callback.invoke() }
                .addOnFailureListener { it.log(TAG) }
    }

    private fun getDeviceLocation() {
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)

        mMap?.let {
            setMapUi(it)
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Utils.AlertHepler.snackbar(this, R.string.snackbar_ask_permission, actionClick = {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
            })
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
        }
    }
}