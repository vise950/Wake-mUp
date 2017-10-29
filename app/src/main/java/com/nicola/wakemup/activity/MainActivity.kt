package com.nicola.wakemup.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.PopupMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
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
import com.nicola.wakemup.preferences.Settings
import com.nicola.wakemup.service.AlarmService
import com.nicola.wakemup.service.GeofenceTransitionsIntentService
import com.nicola.wakemup.utils.Constant.Companion.INVALID_DOUBLE_VALUE
import com.nicola.wakemup.utils.Constant.Companion.INVALID_FLOAT_VALUE
import com.nicola.wakemup.utils.PreferencesHelper
import com.nicola.wakemup.utils.Utils
import com.nicola.wakemup.utils.error
import com.nicola.wakemup.utils.hideKeyboard
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_details.*
import kotlinx.android.synthetic.main.map.*
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar

class MainActivity : BaseActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private val TAG = "ALARM MAP"
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    private val DEFAULT_ZOOM: Float = 0F
    private val ZOOM: Float = 8F
    private val GEOFENCE_REQ_ID = "Alarm map geofence"
    private val GEO_DURATION = 60 * 60 * 1000L

    private val mBottomSheetBehavior by lazy { BottomSheetBehavior.from(bottom_sheet) }
    private var mPopupMenu: PopupMenu? = null

    private val mFusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val mSettingsClient by lazy { SettingsClient(this) }
    private val mLocationRequest by lazy {
        LocationRequest().apply {
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
    private val mLocationSettingRequest by lazy { LocationSettingsRequest.Builder() }
    private lateinit var mLocationCallback: LocationCallback
    private var mLocation: Location? = null

    private val mGoogleApiClient by lazy<GoogleApiClient> {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .build()
    }

    private var mMarker: Marker? = null
    private var mMap: GoogleMap? = null
    private var mCircle: Circle? = null
    private val mCircleOptions by lazy {
        CircleOptions().strokeWidth(10F)
                .strokeColor(ContextCompat.getColor(this@MainActivity, R.color.stoke_map))
                .fillColor(ContextCompat.getColor(this@MainActivity, R.color.fill_map))
    }
    private var mRadius: Double? = null
    private val mGeofence by lazy<Geofence> {
        Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(mMarker?.position?.latitude ?: INVALID_DOUBLE_VALUE, mMarker?.position?.longitude ?: INVALID_DOUBLE_VALUE, mRadius?.toFloat() ?: INVALID_FLOAT_VALUE)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
    }
    private val mGeofenceRequest by lazy<GeofencingRequest> {
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

    private var isThemeChanged: Boolean? = null
    private var isISU: Boolean? = true     // International System of Unit, true is meters, false is miles
    private var isAppRunning: Boolean? = false
    private var mapStyle: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMap()
        initUi()
        PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, true)
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient.connect()
    }

    override fun onResume() {
        super.onResume()
        if (!mGoogleApiClient.isConnected) {
            mGoogleApiClient.reconnect()
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
        if (mGoogleApiClient.isConnected) {
            mGoogleApiClient.disconnect()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            this.mMap = it
            it.setOnMapLongClickListener(this)
            setMapStyle()
        }
    }

    override fun onMapLongClick(latLng: LatLng?) {
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        latLng?.let {
            clearMap(it)
            Utils.LocationHelper.getLocationName(this, it.latitude, it.longitude, {
                place_autocomplete_tv.setText(it)
                place_autocomplete_tv.dismissDropDown()
            })
        }
    }

    override fun onConnected(bundle: Bundle?) = permissionRequest()
    override fun onConnectionFailed(result: ConnectionResult) = Unit
    override fun onConnectionSuspended(i: Int) {}

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.maps_style -> dropdownMenu()
            R.id.settings -> {
                startActivity(Intent(this, Settings::class.java))
                Handler().postDelayed({ mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED }, 200)
            }
            R.id.credits -> {
                startActivity(Intent(this, AboutActivity::class.java))
                Handler().postDelayed({ mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED }, 200)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun dropdownMenu() {
        if (mPopupMenu == null) {
            mPopupMenu = PopupMenu(this, findViewById(R.id.maps_style))
            mPopupMenu?.menuInflater?.inflate(R.menu.maps_menu, mPopupMenu?.menu)
        }

        //fixme its very ugly
        when (mapStyle) {
            0 -> mPopupMenu?.menu?.findItem(R.id.standard)?.isChecked = true
            1 -> mPopupMenu?.menu?.findItem(R.id.standard_dark)?.isChecked = true
            2 -> mPopupMenu?.menu?.findItem(R.id.satellite)?.isChecked = true
        }
        mPopupMenu?.show()
        mPopupMenu?.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.standard -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        mMap?.setMapStyle(null)
                        mapStyle = 0
                    }
                }
                R.id.standard_dark -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_dark_style))
                        mapStyle = 1
                    }
                }
                R.id.satellite -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        mMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                        mapStyle = 2
                    }
                }
            }
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            true
        }
    }

    private fun setViewColor() {
        radius_seekbar?.apply {
            setThumbColor(Color.parseColor(BaseActivity.mAccentColor), Color.parseColor(BaseActivity.mAccentColor))
            setScrubberColor(Color.parseColor(BaseActivity.mAccentColor))
        }
    }

    private fun setMapStyle() {
        mapStyle = PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_MAP_STYLE, -1) as Int
        when (mapStyle) {
        // map style normal
            0 -> {
                mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                mMap?.setMapStyle(null)
            }
        // map style dark
            1 -> {
                mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_dark_style))
            }
        // map style hybrid
            2 -> {
                mMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
            mLocation?.let { map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))) }
            true
        }

        mLocation?.let {
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
                    .targets(TapTarget.forView(map.view, getString(R.string.target_map))
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
                                1 -> mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                                2 -> mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
        mBottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
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
                            setBackgroundColor(Color.parseColor(BaseActivity.mPrimaryColor))
                            setTextColor(ContextCompat.getColor(this@MainActivity,
                                    if (Color.parseColor(BaseActivity.mPrimaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
                            setHintTextColor(ContextCompat.getColor(this@MainActivity,
                                    if (Color.parseColor(BaseActivity.mPrimaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
                        }
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = if (slideOffset > 0) {
                place_autocomplete_tv?.apply {
                    setBackgroundColor(Color.parseColor(BaseActivity.mPrimaryColor))
                    setTextColor(ContextCompat.getColor(this@MainActivity,
                            if (Color.parseColor(BaseActivity.mPrimaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
                    setHintTextColor(ContextCompat.getColor(this@MainActivity,
                            if (Color.parseColor(BaseActivity.mPrimaryColor) == white) R.color.color_primary_text else R.color.color_primary_text_inverse))
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
                mMarker?.let {
                    mCircle?.remove()
                    if (isISU == false) {
                        mCircleOptions?.radius(Utils.milesToMeters(progress))
                    } else {
                        mCircleOptions?.radius(progress * 1000.0)
                    }
                    mRadius = mCircleOptions?.radius
                    mCircle = mMap?.addCircle(mCircleOptions)
                }
            }
        })

        place_autocomplete_tv?.threshold = 2
        val adapter = PlaceAutocompleteAdapter(this, android.R.layout.simple_list_item_1, mGoogleApiClient)
        place_autocomplete_tv?.setAdapter(adapter)
        place_autocomplete_tv.setOnItemClickListener { _, _, i, _ ->
            val item = adapter.getItem(i)
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, item?.placeId?.toString())
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
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            if (place_autocomplete_tv?.text?.isNotEmpty() == true) {
                place_autocomplete_tv.text = null
                mMarker?.remove()
                mCircle?.remove()
                radius_seekbar.progress = radius_seekbar.min
                radius_seekbar.isEnabled = false
            }
        }

        alarm_sound_check.setOnCheckedChangeListener { _, checked ->
            PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, checked)
        }

        fab.setOnClickListener {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            this.hideKeyboard()

            mMarker?.let {
                if (place_autocomplete_tv.text.isNotEmpty() && place_autocomplete_tv.text.toString() != getString(R.string.unknown_place)) {
                    mLocation?.let {
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
        mRadius = 0.0
        radius_seekbar.progress = radius_seekbar.min
        radius_seekbar.isEnabled = true
        mMarker?.remove()
        mCircle?.remove()
        mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(latLng.latitude, latLng.longitude)))
        mCircleOptions?.center(mMarker?.position)
        mCircleOptions?.radius(radius_seekbar.min * 1000.0) //min radius
        mRadius = mCircleOptions?.radius
        mCircle = mMap?.addCircle(mCircleOptions)
        mMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        mGeofenceClient.addGeofences(mGeofenceRequest, mGeoFencePendingIntent)
                .addOnSuccessListener {
                    Utils.AlertHelper.snackbar(this, R.string.snackbar_service_start)
                    PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ADD_GEOFENCE, true)
                    Handler().postDelayed({ finishAndRemoveTask() }, 2000)
                }
                .addOnFailureListener { it.error(TAG) }
    }

    fun removeGeofence(callback: (() -> Unit)? = null) {
        mGeofenceClient.removeGeofences(mGeoFencePendingIntent)
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
                        it.shouldShowRequestPermissionRationale -> Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission,
                                actionMessage = R.string.action_Ok, actionClick = {
                            permissionRequest()
                        })
                        else -> Utils.PermissionHelper.gotoSetting(this)
                    }
                })
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult?.let {
                    mLocation = it.lastLocation
                }
            }
        }

        mFusedLocationClient?.lastLocation?.addOnCompleteListener {
            it.result?.let {
                mLocation = it
                mMap?.let { setMapUi(it) }
            }
        }
    }
}