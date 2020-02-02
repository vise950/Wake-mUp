package com.nicola.wakemup.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nicola.wakemup.R
import com.nicola.wakemup.preferences.SettingsActivity
import com.nicola.wakemup.service.AddressReceiver
import com.nicola.wakemup.service.AlarmService
import com.nicola.wakemup.service.GeofenceTransitionsIntentService
import com.nicola.wakemup.ui.fragment.MapFragment
import com.nicola.wakemup.utils.*
import kotlinx.android.synthetic.main.bottom_details.*

class MainActivity : AppCompatActivity(),
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    companion object {
        private val TAG = "ALARM MAP"
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

        private val GEOFENCE_REQ_ID = "Alarm map geofence"
        private val GEO_DURATION = 60 * 60 * 1000L
    }

    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(bottom_sheet) }

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val locationRequest by lazy {
        LocationRequest().apply {
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
    private val locationSettingRequest by lazy {
        LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build()
    }
    private var location: Location? = null

    private val googleApiClient by lazy<GoogleApiClient> {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .build()
    }

    private var marker: Marker? = null
    private var circle: Circle? = null
    private val circleOptions by lazy {
        CircleOptions().strokeWidth(10F)
                .strokeColor(ContextCompat.getColor(this@MainActivity, R.color.stoke_map))
                .fillColor(ContextCompat.getColor(this@MainActivity, R.color.fill_map))
    }
    private var radius: Double? = null
    private val geofence by lazy<Geofence> {
        Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(marker?.position?.latitude ?: INVALID_DOUBLE_VALUE,
                        marker?.position?.longitude ?: INVALID_DOUBLE_VALUE,
                        radius?.toFloat() ?: INVALID_FLOAT_VALUE)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
    }
    private val geofenceRequest by lazy<GeofencingRequest> {
        GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
    }
    private val geoFencePendingIntent by lazy<PendingIntent> {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private val geofenceClient by lazy { LocationServices.getGeofencingClient(this) }

    private val addressReceiver by lazy { AddressReceiver() }

    private var isISU: Boolean? = true     // International System of Unit, true is meters, false is miles
    private var isAppRunning: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initLocation()
//        initUi()

        supportFragmentManager.beginTransaction()
                .add(R.id.map_fragment, MapFragment()).commit()
    }

    override fun onStart() {
        super.onStart()
        googleApiClient.connect()
    }

    override fun onResume() {
        super.onResume()
        if (!googleApiClient.isConnected) {
            googleApiClient.reconnect()
        }
//
//        isISU = PreferencesHelper.isISU(this)
//        radius_txt.text = String.format(resources.getString(R.string.text_radius), PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_DISTANCE, "km"))
//
//        if (Utils.isMyServiceRunning(this, AlarmService::class.java)) {
//            stopService(Intent(this, AlarmService::class.java))
//        }
    }

    override fun onStop() {
        super.onStop()
        if (googleApiClient.isConnected) {
            googleApiClient.disconnect()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION) {
            if (permissions.isNotEmpty() && isPermissionGranted(permissions.first()))
                getDeviceLocation()
            else
                Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission,
                        actionMessage = R.string.action_Ok, actionClick = { permissionRequest() })
        }
    }

    override fun onConnected(bundle: Bundle?) {
        if (Utils.isLocationGranted(this)) getDeviceLocation()
    }

    override fun onConnectionFailed(result: ConnectionResult) = Unit
    override fun onConnectionSuspended(i: Int) {}
    override fun onLocationChanged(l: Location?) {}

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                Handler().postDelayed({ bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED }, 200)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initLocation() {
        LocationServices.getSettingsClient(this)
                .checkLocationSettings(locationSettingRequest)
                .addOnCompleteListener {
                    try {
                        it.getResult(ApiException::class.java)
                    } catch (exception: ApiException) {
                        when (exception.statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                (exception as ResolvableApiException)
                                        .startResolutionForResult(this, LOCATION_SETTINGS)
                            }
                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                //todo snackbar
                                // goto Location setting
                                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        }
                    }
                }
                .addOnSuccessListener { permissionRequest() }
    }

//    private fun initUi() {
//        //workaround for intercept drag map view and disable it
//        view.setOnTouchListener { _, _ -> true }
//
//        view.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
//
//        val white = -3 //my white is -3 but Color.WHITE is -1
//        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                when (newState) {
//                    BottomSheetBehavior.STATE_COLLAPSED -> {
//                        bottomSheet.isEnabled = false
//                        place_autocomplete_tv?.apply {
//                            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
//                            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text))
//                            setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))
//                        }
//                        this@MainActivity.hideKeyboard()
//                    }
//                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        place_autocomplete_tv?.apply {
//                            //                            setBackgroundColor(Color.parseColor(BaseActivity.primaryColor))
////                            setTextColor(ContextCompat.getColor(this@MainActivity,
////                                    if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
////                            setHintTextColor(ContextCompat.getColor(this@MainActivity,
////                                    if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
//                        }
//                    }
//                }
//            }
//
//            override fun onSlide(bottomSheet: View, slideOffset: Float) = if (slideOffset > 0) {
//                place_autocomplete_tv?.apply {
//                    //                    setBackgroundColor(Color.parseColor(BaseActivity.primaryColor))
////                    setTextColor(ContextCompat.getColor(this@MainActivity,
////                            if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
////                    setHintTextColor(ContextCompat.getColor(this@MainActivity,
////                            if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
//                }
//                // wtf android
//                val a = 0
//            } else {
//                place_autocomplete_tv?.apply {
//                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
//                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text))
//                    setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))
//                }
//                this@MainActivity.hideKeyboard()
//            }
//        })
//
//        radius_seekbar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
//            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) = Unit
//            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) = Unit
//            override fun onProgressChanged(seekBar: DiscreteSeekBar?, progress: Int, fromUser: Boolean) {
//                marker?.let {
//                    circle?.remove()
//                    if (isISU == false) {
//                        circleOptions?.radius(Utils.milesToMeters(progress))
//                    } else {
//                        circleOptions?.radius(progress * 1000.0)
//                    }
//                    radius = circleOptions?.radius
//                    circle = map?.addCircle(circleOptions)
//                }
//            }
//        })
//
//        place_autocomplete_tv?.threshold = 2
//        val adapter = PlaceAutocompleteAdapter(this, android.R.layout.simple_list_item_1, googleApiClient)
//        place_autocomplete_tv?.setAdapter(adapter)
//        place_autocomplete_tv.setOnItemClickListener { _, _, i, _ ->
//            val item = adapter.getItem(i)
//            Places.GeoDataApi.getPlaceById(googleApiClient, item?.placeId?.toString())
//                    .setResultCallback {
//                        if (it.status?.isSuccess == true) {
//                            clearMap(it.get(0).latLng)
//                        } else {
//                            "Error place".error(TAG)
//                        }
//                        it.release()
//                    }
//        }
//
//        place_autocomplete_tv.setOnClickListener {
//            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
//            if (place_autocomplete_tv?.text?.isNotEmpty() == true) {
//                place_autocomplete_tv.text = null
//                marker?.remove()
//                circle?.remove()
//                radius_seekbar.progress = radius_seekbar.min
//                radius_seekbar.isEnabled = false
//            }
//        }
//
//        alarm_sound_check.setOnCheckedChangeListener { _, checked ->
//            PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, checked)
//        }
//
//        fab.setOnClickListener {
//            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
//            this.hideKeyboard()
//
//            marker?.let {
//                if (place_autocomplete_tv.text.isNotEmpty() && place_autocomplete_tv.text.toString() != getString(R.string.unknown_place)) {
//                    location?.let {
//                        if (PreferencesHelper.isAnotherGeofenceActive(this) == true) {
//                            Handler().postDelayed({
//                                Utils.AlertHelper.dialog(this, R.string.dialog_title_another_service, R.string.dialog_message_another_service, {
//                                    removeGeofence({ addGeofence() })
//                                })
//                            }, 300)
//                        } else {
//                            addGeofence()
//                        }
//                    }
//                } else {
//                    Utils.AlertHelper.snackbar(this, R.string.snackbar_no_location, duration = Snackbar.LENGTH_LONG)
//                }
//            }
//        }
//    }
//
//    private fun clearMap(latLng: LatLng) {
//        this.hideKeyboard()
//        radius = 0.0
//        radius_seekbar.progress = radius_seekbar.min
//        radius_seekbar.isEnabled = true
//        marker?.remove()
//        circle?.remove()
//        marker = map?.addMarker(MarkerOptions().position(LatLng(latLng.latitude, latLng.longitude)))
//        circleOptions?.center(marker?.position)
//        circleOptions?.radius(radius_seekbar.min * 1000.0) //min radius
//        radius = circleOptions?.radius
//        circle = map?.addCircle(circleOptions)
//        map?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
//    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        geofenceClient.addGeofences(geofenceRequest, geoFencePendingIntent)
                .addOnSuccessListener {
                    Utils.AlertHelper.snackbar(this, R.string.snackbar_service_start)
                    PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ADD_GEOFENCE, true)
                    Handler().postDelayed({ finishAndRemoveTask() }, 2000)
                }
                .addOnFailureListener { it.log(TAG) }
    }

    fun removeGeofence(callback: (() -> Unit)? = null) {
        geofenceClient.removeGeofences(geoFencePendingIntent)
                .addOnSuccessListener { callback?.invoke() }
                .addOnFailureListener { it.log(TAG) }
    }

    @SuppressLint("NewApi")
    private fun permissionRequest() {
        if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            getDeviceLocation()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        fusedLocationClient?.lastLocation?.addOnCompleteListener {
            it.result?.let {
                locationUpdated.invoke(it)
//                location = it
//                map?.let { setMapUi(it) }
            }
        }
    }
}