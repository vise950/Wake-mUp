package com.nicola.wakemup.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.PopupMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.nicola.wakemup.BuildConfig
import com.nicola.wakemup.R
import com.nicola.wakemup.adapter.PlaceAutocompleteAdapter
import com.nicola.wakemup.preferences.SettingsActivity
import com.nicola.wakemup.service.AddressReceiver
import com.nicola.wakemup.service.AlarmService
import com.nicola.wakemup.service.FetchAddressIntentService
import com.nicola.wakemup.service.GeofenceTransitionsIntentService
import com.nicola.wakemup.utils.*
import com.nicola.wakemup.utils.Constants.Companion.INVALID_DOUBLE_VALUE
import com.nicola.wakemup.utils.Constants.Companion.INVALID_FLOAT_VALUE
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_details.*
import kotlinx.android.synthetic.main.map.*
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

class MainActivity : BaseActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    companion object {
        private val TAG = "ALARM MAP"
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private val DEFAULT_ZOOM: Float = 0F
        private val ZOOM: Float = 8F
        private val GEOFENCE_REQ_ID = "Alarm map geofence"
        private val GEO_DURATION = 60 * 60 * 1000L
    }

    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(bottom_sheet) }
    private var popupMenu: PopupMenu? = null

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
    private var map: GoogleMap? = null
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

    private var isThemeChanged: Boolean? = null
    private var isISU: Boolean? = true     // International System of Unit, true is meters, false is miles
    private var isAppRunning: Boolean? = false
    private var mapStyle: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initLocation()
        initMap()
        initUi()
        PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, true)
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

        setViewColor()

        isISU = PreferencesHelper.isISU(this)
        radius_txt.text = String.format(resources.getString(R.string.text_radius), PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_DISTANCE, "km"))

        if (Utils.isMyServiceRunning(this, AlarmService::class.java)) {
            stopService(Intent(this, AlarmService::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
        PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_MAP_STYLE, mapStyle)
    }

    override fun onStop() {
        super.onStop()
        if (googleApiClient.isConnected) {
            googleApiClient.disconnect()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.LOCATION_SETTINGS ->
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        permissionRequest()
                    }
                    Activity.RESULT_CANCELED -> {
                        //todo maybe change text
                        Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission,
                                actionMessage = R.string.action_Ok, actionClick = { initLocation() })
                    }
                }
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let { pennywise ->
            this.map = pennywise
            pennywise.setOnMapLongClickListener(this)
            setMapStyle()
        }
    }

    override fun onMapLongClick(latLng: LatLng?) {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        latLng?.let {
            clearMap(it)

            Intent(this, FetchAddressIntentService::class.java).apply {
                putExtra(Constants.RECEIVER, addressReceiver)
                putExtra(Constants.LOCATION_DATA_EXTRA, it)
            }.let {
                        startService(it)
                    }

            addressReceiver.onResultReceive = {
                runOnUiThread {
                    place_autocomplete_tv.setText(it)
                    place_autocomplete_tv.dismissDropDown()
                }
            }
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
            R.id.maps_style -> dropdownMenu()
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                Handler().postDelayed({ bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED }, 200)
            }
            R.id.credits -> {
                startActivity(Intent(this, AboutActivity::class.java))
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
                                        .startResolutionForResult(this, Constants.LOCATION_SETTINGS)
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

    private fun dropdownMenu() {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, findViewById(R.id.maps_style))
            popupMenu?.menuInflater?.inflate(R.menu.maps_menu, popupMenu?.menu)
        }

        //fixme its very ugly
        when (mapStyle) {
            0 -> popupMenu?.menu?.findItem(R.id.standard)?.isChecked = true
            1 -> popupMenu?.menu?.findItem(R.id.standard_dark)?.isChecked = true
            2 -> popupMenu?.menu?.findItem(R.id.satellite)?.isChecked = true
        }
        popupMenu?.show()
        popupMenu?.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.standard -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        map?.setMapStyle(null)
                        mapStyle = 0
                    }
                }
                R.id.standard_dark -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        map?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_dark_style))
                        mapStyle = 1
                    }
                }
                R.id.satellite -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        map?.mapType = GoogleMap.MAP_TYPE_HYBRID
                        mapStyle = 2
                    }
                }
            }
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            true
        }
    }

    private fun setViewColor() {
        radius_seekbar?.apply {
            setThumbColor(Color.parseColor(BaseActivity.accentColor), Color.parseColor(BaseActivity.accentColor))
            setScrubberColor(Color.parseColor(BaseActivity.accentColor))
        }
    }

    private fun setMapStyle() {
        mapStyle = PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_MAP_STYLE, -1) as Int
        when (mapStyle) {
        // map style normal
            0 -> {
                map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                map?.setMapStyle(null)
            }
        // map style dark
            1 -> {
                map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                map?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_dark_style))
            }
        // map style hybrid
            2 -> {
                map?.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
        }
    }


    private fun initMap() {
        (supportFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment).getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    private fun setMapUi(map: GoogleMap) {
        map.apply {
            isMyLocationEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isMapToolbarEnabled = false
        }

        map.setOnMyLocationButtonClickListener {
            location?.let { map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))) }
            true
        }

        location?.let {
            if (isAppRunning == false) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM))
                map.animateCamera(CameraUpdateFactory.zoomTo(ZOOM), 2000, null)
                isAppRunning = true
            }
        }
    }

    private fun initShowCase() {
        if (PreferencesHelper.isShowCase(this) == true) {
            TapTargetSequence(this)
                    .targets(TapTarget.forView(google_map.view, getString(R.string.target_map))
                            .outerCircleColor(R.color.blue_grey_500)
                            .transparentTarget(true)
                            .textColor(R.color.color_primary_text_inverse)
                            .cancelable(false),
                            TapTarget.forView(place_autocomplete_tv, getString(R.string.target_edit))
                                    .id(1)
                                    .outerCircleColor(R.color.blue_grey_500)
                                    .transparentTarget(true)
                                    .textColor(R.color.color_primary_text_inverse)
                                    .cancelable(false),
                            TapTarget.forView(radius_seekbar, getString(R.string.target_seekbar))
                                    .id(2)
                                    .outerCircleColor(R.color.blue_grey_500)
                                    .transparentTarget(true)
                                    .textColor(R.color.color_primary_text_inverse)
                                    .cancelable(false),
                            TapTarget.forView(fab, getString(R.string.target_fab))
                                    .outerCircleColor(R.color.blue_grey_500)
                                    .transparentTarget(true)
                                    .textColor(R.color.color_primary_text_inverse)
                                    .cancelable(false))
                    .listener(object : TapTargetSequence.Listener {
                        override fun onSequenceCanceled(lastTarget: TapTarget) = Unit
                        override fun onSequenceFinish() =
                                PreferencesHelper.setPreferences(this@MainActivity, PreferencesHelper.KEY_SHOW_CASE, false)

                        override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {
                            when (lastTarget.id()) {
                                1 -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                                2 -> bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            }
                        }
                    })
                    .start()
        }
    }

    private fun initUi() {
        isThemeChanged = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_THEME, false) as Boolean

        //workaround for intercept drag map view and disable it
        view.setOnTouchListener { _, _ -> true }

        view.setBackgroundColor(ContextCompat.getColor(this@MainActivity, if (isThemeChanged == true) R.color.dark else R.color.white))

        val white = -3 //my white is -3 but Color.WHITE is -1
        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        bottomSheet.isEnabled = false
                        place_autocomplete_tv?.apply {
                            setBackgroundColor(ContextCompat.getColor(this@MainActivity, if (isThemeChanged == true) R.color.dark else R.color.white))
                            setTextColor(ContextCompat.getColor(this@MainActivity, if (isThemeChanged == true) R.color.color_primary_text_inverse else R.color.color_primary_text))
                            setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))
                        }
                        this@MainActivity.hideKeyboard()
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        place_autocomplete_tv?.apply {
                            setBackgroundColor(Color.parseColor(BaseActivity.primaryColor))
                            setTextColor(ContextCompat.getColor(this@MainActivity,
                                    if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
                            setHintTextColor(ContextCompat.getColor(this@MainActivity,
                                    if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
                        }
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = if (slideOffset > 0) {
                place_autocomplete_tv?.apply {
                    setBackgroundColor(Color.parseColor(BaseActivity.primaryColor))
                    setTextColor(ContextCompat.getColor(this@MainActivity,
                            if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
                    setHintTextColor(ContextCompat.getColor(this@MainActivity,
                            if (Color.parseColor(BaseActivity.primaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
                }
                // wtf android
                val a = 0
            } else {
                place_autocomplete_tv?.apply {
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, if (isThemeChanged == true) R.color.dark else R.color.white))
                    setTextColor(ContextCompat.getColor(this@MainActivity, if (isThemeChanged == true) R.color.color_primary_text_inverse else R.color.color_primary_text))
                    setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))
                }
                this@MainActivity.hideKeyboard()
            }
        })

        radius_seekbar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) = Unit
            override fun onProgressChanged(seekBar: DiscreteSeekBar?, progress: Int, fromUser: Boolean) {
                marker?.let {
                    circle?.remove()
                    if (isISU == false) {
                        circleOptions?.radius(Utils.milesToMeters(progress))
                    } else {
                        circleOptions?.radius(progress * 1000.0)
                    }
                    radius = circleOptions?.radius
                    circle = map?.addCircle(circleOptions)
                }
            }
        })

        place_autocomplete_tv?.threshold = 2
        val adapter = PlaceAutocompleteAdapter(this, android.R.layout.simple_list_item_1, googleApiClient)
        place_autocomplete_tv?.setAdapter(adapter)
        place_autocomplete_tv.setOnItemClickListener { _, _, i, _ ->
            val item = adapter.getItem(i)
            Places.GeoDataApi.getPlaceById(googleApiClient, item?.placeId?.toString())
                    .setResultCallback {
                        if (it.status?.isSuccess == true) {
                            clearMap(it.get(0).latLng)
                        } else {
                            "Error place".error(TAG)
                        }
                        it.release()
                    }
        }

        place_autocomplete_tv.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            if (place_autocomplete_tv?.text?.isNotEmpty() == true) {
                place_autocomplete_tv.text = null
                marker?.remove()
                circle?.remove()
                radius_seekbar.progress = radius_seekbar.min
                radius_seekbar.isEnabled = false
            }
        }

        alarm_sound_check.setOnCheckedChangeListener { _, checked ->
            PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, checked)
        }

        fab.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            this.hideKeyboard()

            marker?.let {
                if (place_autocomplete_tv.text.isNotEmpty() && place_autocomplete_tv.text.toString() != getString(R.string.unknown_place)) {
                    location?.let {
                        if (PreferencesHelper.isAnotherGeofenceActive(this) == true) {
                            Handler().postDelayed({
                                Utils.AlertHelper.dialog(this, R.string.dialog_title_another_service, R.string.dialog_message_another_service, {
                                    removeGeofence({ addGeofence() })
                                })
                            }, 300)
                        } else {
                            addGeofence()
                        }
                    }
                } else {
                    Utils.AlertHelper.snackbar(this, R.string.snackbar_no_location, duration = Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    private fun clearMap(latLng: LatLng) {
        this.hideKeyboard()
        radius = 0.0
        radius_seekbar.progress = radius_seekbar.min
        radius_seekbar.isEnabled = true
        marker?.remove()
        circle?.remove()
        marker = map?.addMarker(MarkerOptions().position(LatLng(latLng.latitude, latLng.longitude)))
        circleOptions?.center(marker?.position)
        circleOptions?.radius(radius_seekbar.min * 1000.0) //min radius
        radius = circleOptions?.radius
        circle = map?.addCircle(circleOptions)
        map?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        geofenceClient.addGeofences(geofenceRequest, geoFencePendingIntent)
                .addOnSuccessListener {
                    Utils.AlertHelper.snackbar(this, R.string.snackbar_service_start)
                    PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ADD_GEOFENCE, true)
                    Handler().postDelayed({ finishAndRemoveTask() }, 2000)
                }
                .addOnFailureListener { it.error(TAG) }
    }

    fun removeGeofence(callback: (() -> Unit)? = null) {
        geofenceClient.removeGeofences(geoFencePendingIntent)
                .addOnSuccessListener { callback?.invoke() }
                .addOnFailureListener { it.error(TAG) }
    }

    private fun permissionRequest() {
        RxPermissions(this)
                .requestEach(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe({
                    when {
                        it.granted -> {
                            getDeviceLocation()
                            if (!BuildConfig.DEBUG) initShowCase()
                        }
                        it.shouldShowRequestPermissionRationale ->
                            Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission,
                                    actionMessage = R.string.action_Ok, actionClick = {
                                permissionRequest()
                            })
                        else -> Utils.PermissionHelper.gotoSetting(this)
                    }
                })
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        fusedLocationClient?.lastLocation?.addOnCompleteListener {
            it.result?.let {
                location = it
                map?.let { setMapUi(it) }
            }
        }
    }
}