package com.nicola.wakemup.activity

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.nicola.wakemup.R
import com.nicola.wakemup.adapter.PlaceAutocompleteAdapter
import com.nicola.wakemup.preferences.Settings
import com.nicola.wakemup.service.AlarmService
import com.nicola.wakemup.service.GeofenceTransitionsIntentService
import com.nicola.wakemup.utils.Constant.Companion.INVALID_DOUBLE_VALUE
import com.nicola.wakemup.utils.Constant.Companion.INVALID_FLOAT_VALUE
import com.nicola.wakemup.utils.Groupie
import com.nicola.wakemup.utils.PreferencesHelper
import com.nicola.wakemup.utils.Utils
import com.nicola.wakemup.utils.error
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
        com.google.android.gms.location.LocationListener {

    companion object {
        private val TAG = "ALARM MAP"
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private val DEFAULT_ZOOM: Float = 0F
        private val ZOOM: Float = 8F
        private val GEOFENCE_REQ_ID = "Alarm map geofence"
        private val GEO_DURATION = 60 * 60 * 1000L
    }

    private val mBottomSheetBehavior by lazy { BottomSheetBehavior.from(bottom_sheet) }
    private var mPopupMenu: PopupMenu? = null

    private val mGoogleApiClient by lazy <GoogleApiClient> {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .build()
    }
    private val mLocationRequest by lazy { LocationRequest() }
    private var mLocation: Location? = null
    private var mMarker: Marker? = null
    private var mMap: GoogleMap? = null
    private var mCircle: Circle? = null
    private val mCircleOptions by lazy {
        CircleOptions().strokeWidth(10F)
                .strokeColor(ContextCompat.getColor(this@MainActivity, R.color.stoke_map))
                .fillColor(ContextCompat.getColor(this@MainActivity, R.color.fill_map))
    }
    private var mRadius: Double? = null
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

    private var isThemeChanged: Boolean? = null
    private var isUISystem: Boolean? = true     // International System of Unit, true is meters, false is miles
    private var isAppRunning: Boolean? = false


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
        if (!mGoogleApiClient.isConnected) {
            mGoogleApiClient.reconnect()
        }

        setViewColor()

        isUISystem = PreferencesHelper.isUISystem(this)
        radius_txt.text = String.format(resources.getString(R.string.text_radius), PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_DISTANCE, "km"))

        if (Utils.isMyServiceRunning(this, AlarmService::class.java)) {
            stopService(Intent(this, AlarmService::class.java))
        }
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
            it.setOnMapLongClickListener(this)
        }
    }

    override fun onMapLongClick(latLng: LatLng?) {
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        mRadius = 0.0
        radius_seekbar.progress = 1
        mMarker?.remove()
        mCircle?.remove()
        Utils.hideKeyboard(this)
        latLng?.let {
            radius_seekbar.isEnabled = true
            mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)))
//            mMarker?.setIcon(Utils.vectorToBitmap(this,R.drawable.ic_alarm_add))
            mMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(21F))
            mCircleOptions?.center(mMarker?.position)
            mCircleOptions?.radius(radius_seekbar.min * 1000.0) //min radius
            mRadius = mCircleOptions?.radius
            mCircle = mMap?.addCircle(mCircleOptions)
            mMap?.animateCamera(CameraUpdateFactory.newLatLng(it))
            Utils.LocationHelper.getLocationName(this, it.latitude, it.longitude, {
                place_autocomplete_tv.setText(it)
                place_autocomplete_tv.dismissDropDown()
            })
        }
    }

    override fun onConnected(bundle: Bundle?) {
        permissionRequest()
    }

    override fun onConnectionFailed(result: ConnectionResult) {}

    override fun onConnectionSuspended(i: Int) {
        mGeoFencePendingIntent.let { LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, it) }
    }

    override fun onLocationChanged(location: Location?) {
        this.mLocation = location
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.maps_style -> dropdownMenu()
            R.id.settings -> {
                startActivity(Intent(this, Settings::class.java))
                Handler().postDelayed({
                    mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                }, 200)
            }
            R.id.credits -> {
                startActivity(Intent(this, AboutActivity::class.java))
                Handler().postDelayed({
                    mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                }, 200)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun dropdownMenu() {
        if (mPopupMenu == null) {
            mPopupMenu = PopupMenu(this, findViewById(R.id.maps_style))
            mPopupMenu?.menuInflater?.inflate(R.menu.maps_menu, mPopupMenu?.menu)
        }
        mPopupMenu?.show()
        mPopupMenu?.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.standard -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        mMap?.setMapStyle(null)
                    }
                }
                R.id.standard_dark -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_dark_style))
                    }
                }
                R.id.satellite -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        mMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                    }
                }
            }
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            true
        }
    }

    private fun setViewColor() {
        radius_seekbar.setThumbColor(Color.parseColor(BaseActivity.mAccentColor), Color.parseColor(BaseActivity.mAccentColor))
        radius_seekbar.setScrubberColor(Color.parseColor(BaseActivity.mAccentColor))
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setMapUi(map: GoogleMap) {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
        map.uiSettings.isMapToolbarEnabled = false

        map.setOnMyLocationButtonClickListener {
            mLocation?.let {
                map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
            }
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
                        override fun onSequenceCanceled(lastTarget: TapTarget) {}
                        override fun onSequenceFinish() {
                            PreferencesHelper.setPreferences(this@MainActivity, PreferencesHelper.KEY_SHOW_CASE, false)
                        }

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
        view.setOnTouchListener { view, motionEvent -> true }

        if (isThemeChanged == true) {
            view.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.dark))
        } else {
            view.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }

        mBottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        bottomSheet.isEnabled = false
                        if (isThemeChanged == true) {
                            place_autocomplete_tv.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.dark))
                            place_autocomplete_tv.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                        } else {
                            place_autocomplete_tv.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                            place_autocomplete_tv.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text))
                        }
                        place_autocomplete_tv.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))

                        Utils.hideKeyboard(this@MainActivity)
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        place_autocomplete_tv.setBackgroundColor(Color.parseColor(BaseActivity.mPrimaryColor))
                        place_autocomplete_tv.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                        place_autocomplete_tv.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) {
                    place_autocomplete_tv.setBackgroundColor(Color.parseColor(BaseActivity.mPrimaryColor))
                    place_autocomplete_tv.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                    place_autocomplete_tv.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                } else {
                    if (isThemeChanged == true) {
                        place_autocomplete_tv.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.dark))
                        place_autocomplete_tv.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                    } else {
                        place_autocomplete_tv.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        place_autocomplete_tv.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text))
                    }
                    place_autocomplete_tv.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))
                }
            }
        })

        radius_seekbar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onProgressChanged(seekBar: DiscreteSeekBar?, progress: Int, fromUser: Boolean) {
                mMarker?.let {
                    mCircle?.remove()
                    if (isUISystem == false) {
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
        place_autocomplete_tv.setOnItemClickListener { adapterView, view, i, l ->
            val item = adapter.getItem(i)
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, item?.placeId?.toString())
                    .setResultCallback {
                        if (it.status?.isSuccess == true) {
                            Utils.hideKeyboard(this)
                            mMarker?.remove()
                            mCircle?.remove()
                            radius_seekbar.isEnabled = true
                            radius_seekbar.min = radius_seekbar.min
                            mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(it.get(0).latLng.latitude, it.get(0).latLng.longitude)))
                            mCircleOptions.center(mMarker?.position)
                            mMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.get(0).latLng.latitude, it.get(0).latLng.longitude)))
                            mMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(21F))
                            mCircleOptions?.radius(radius_seekbar.min * 1000.0) //min radius
                            mRadius = mCircleOptions?.radius
                            mCircle = mMap?.addCircle(mCircleOptions)
                        } else {
                            "Error place".error(TAG)
                        }
                        it.release()
                    }
        }

        place_autocomplete_tv.setOnClickListener {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            if (place_autocomplete_tv?.text?.isNotEmpty() ?: false) {
                place_autocomplete_tv.text = null
                mMarker?.remove()
                mCircle?.remove()
                radius_seekbar.progress = radius_seekbar.min
                radius_seekbar.isEnabled = false
            }
        }

        alarm_sound_check.setOnCheckedChangeListener { compoundButton, checked ->
            PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, checked)
        }

        fab.setOnClickListener {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            Utils.hideKeyboard(this)

            mMarker?.let {
                if (place_autocomplete_tv.text.isNotEmpty() && place_autocomplete_tv.text.toString() != getString(R.string.unknown_place)) {
                    mLocation?.let {
                        if (PreferencesHelper.isAnotherGeofenceActived(this) == true) {
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

    private fun addGeofence() {
        mGeofenceClient.addGeofences(mGeofenceRequest, mGeoFencePendingIntent)
                .addOnSuccessListener {
                    Utils.AlertHelper.snackbar(this, R.string.snackbar_service_start)
                    PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ADD_GEOFENCE, true)
                    Handler().postDelayed({ finishAndRemoveTask() }, 2000)
                }
                .addOnFailureListener { it.error(TAG) }
    }

    private fun removeGeofence(callback: (() -> Unit)) {
        mGeofenceClient.removeGeofences(mGeoFencePendingIntent)
                .addOnSuccessListener { callback.invoke() }
                .addOnFailureListener { it.error(TAG) }
    }

    private fun permissionRequest() {
        RxPermissions(this)
                .requestEach(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe({
                    if (it.granted) {
                        getDeviceLocation()
                        initShowCase()
                    } else if (it.shouldShowRequestPermissionRationale) {
                        Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission,
                                actionMessage = R.string.action_Ok, actionClick = {
                            permissionRequest()
                        })
                    } else {
                        Utils.PermissionHelper.gotoSetting(this)
                    }
                })
    }

    private fun getDeviceLocation() {
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)

        mMap?.let { setMapUi(it) }
    }
}