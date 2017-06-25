package com.nicola.alarmap.ui.activity

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.media.AudioManager
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.AestheticActivity
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
import com.nicola.alarmap.preferences.Credits
import com.nicola.alarmap.preferences.Settings
import com.nicola.alarmap.service.AlarmService
import com.nicola.alarmap.service.GeofenceTransitionsIntentService
import com.nicola.alarmap.utils.Constant.Companion.INVALID_DOUBLE_VALUE
import com.nicola.alarmap.utils.Constant.Companion.INVALID_FLOAT_VALUE
import com.nicola.alarmap.utils.Groupie
import com.nicola.alarmap.utils.PreferencesHelper
import com.nicola.alarmap.utils.Utils
import com.nicola.alarmap.utils.log
import com.seatgeek.placesautocomplete.model.AutocompleteResultType
import com.nicola.com.alarmap.R
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_details.*
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar
import java.util.*

class MainActivity : AestheticActivity(),
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
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
    }

    private val mBottomSheetBehavior by lazy { BottomSheetBehavior.from(bottom_sheet) }
    private var mMap: GoogleMap? = null
    private var mCircle: Circle? = null
    private val mCircleOptions by lazy {
        CircleOptions()
                .center(LatLng(mMarker?.position?.latitude ?: INVALID_DOUBLE_VALUE, mMarker?.position?.longitude ?: INVALID_DOUBLE_VALUE))
                .strokeWidth(10F)
                .strokeColor(ContextCompat.getColor(this@MainActivity, R.color.stoke_map))
                .fillColor(ContextCompat.getColor(this@MainActivity, R.color.fill_map))
    }
    private var mRadius: Double? = null
    private var mRequestPermissionCount = 0
    private var isBusSelected = true
    private var isTrainSelected = false
    private var isPlaneSelected = false

    private var mDisposable: Disposable? = null

    private val mGoogleApiClient by lazy <GoogleApiClient> {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build()
    }

    private val mLocationRequest by lazy { LocationRequest() }
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

    private val mAudioManager by lazy { this.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val mAlarmVolume by lazy { mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) }

    private var mPrimaryColor: String? = null
    private var mAccentColor: String? = null
    private val mUnselectedButtonBg by lazy { getDrawable(R.drawable.btn_background) as GradientDrawable }
    private val mSelectedButtonBg by lazy { getDrawable(R.drawable.btn_background) as GradientDrawable }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getColor()
        Aesthetic.get()
                .colorPrimary(Color.parseColor(mPrimaryColor))
                .colorAccent(Color.parseColor(mAccentColor))
                .colorStatusBarAuto()
                .textColorPrimaryRes(R.color.color_primary_text)
                .textColorSecondaryRes(R.color.color_secondary_text)
                .isDark(false)
                .apply()
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

        getColor()

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
        mMarker?.remove()
        mCircle?.remove()
        radius_seekbar.isEnabled = true
        Utils.hideKeyboard(this)
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

    override fun onConnectionFailed(result: ConnectionResult) {}
    override fun onConnectionSuspended(i: Int) {
        mGeoFencePendingIntent.let { LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, it) }
    }

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
                        Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission, actionClick = {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
                        })
                        mRequestPermissionCount++
                    } else {
                        Utils.AlertHelper.snackbar(this, R.string.snackbar_permission_denied, duration = Snackbar.LENGTH_LONG)
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings -> startActivity(Intent(this, Settings::class.java))
            R.id.credits -> startActivity(Intent(this, Credits::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getColor() {
        mPrimaryColor = Utils.getParseColor(this, PreferencesHelper.KEY_PRIMARY_COLOR)
        mAccentColor = Utils.getParseColor(this, PreferencesHelper.KEY_ACCENT_COLOR)
        mSelectedButtonBg.setColor(Color.parseColor(mAccentColor))
        mUnselectedButtonBg.setColor(Color.WHITE)
        radius_seekbar.setThumbColor(Color.parseColor(mAccentColor), Color.parseColor(mAccentColor))
        radius_seekbar.setScrubberColor(Color.parseColor(mAccentColor))
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
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), ZOOM))
            }
            true
        }

        mLocation?.let {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM))
            map.animateCamera(CameraUpdateFactory.zoomTo(ZOOM), 1000, null)
        }
    }

    private fun initUi() {

        //workaround for intercept drag map view and disable it
        view.setOnTouchListener { view, motionEvent -> true }

        mBottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        bottomSheet.isEnabled = false
                        destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text))
                        destination_txt.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))

                        Utils.hideKeyboard(this@MainActivity)
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        bottomSheet.isEnabled = true
                        destination_txt.setBackgroundColor(Color.parseColor(mPrimaryColor))
                        destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                        destination_txt.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) {
                    destination_txt.setBackgroundColor(Color.parseColor(mPrimaryColor))
                    destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                    destination_txt.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text_inverse))
                } else {
                    destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    destination_txt.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary_text))
                    destination_txt.setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.color_secondary_text))
                }
            }
        })

        bus_btn.background = mSelectedButtonBg
        bus_btn.setImageDrawable(getDrawable(R.drawable.ic_bus_white))
        Groupie(bus_btn, train_btn, plane_btn).setOnClickListener {
            when (it.id) {
                R.id.bus_btn -> {
                    if (isTrainSelected || isPlaneSelected) {
                        Groupie(train_btn,plane_btn).setBackground(mUnselectedButtonBg)
                        train_btn.setImageDrawable(getDrawable(R.drawable.ic_train_black))
                        plane_btn.setImageDrawable(getDrawable(R.drawable.ic_plane_black))
                        isTrainSelected = false
                        isPlaneSelected = false
                    }
                    if (!isBusSelected) {
                        bus_btn.background = mSelectedButtonBg
                        bus_btn.setImageDrawable(getDrawable(R.drawable.ic_bus_white))
                        isBusSelected = true
                        radius_seekbar.min = 1
                        radius_seekbar.max = 50
                        radius_seekbar.progress = radius_seekbar.min
                    }
                }
                R.id.train_btn -> {
                    if (isBusSelected || isPlaneSelected) {
                        Groupie(bus_btn,plane_btn).setBackground(mUnselectedButtonBg)
                        bus_btn.setImageDrawable(getDrawable(R.drawable.ic_bus_black))
                        plane_btn.setImageDrawable(getDrawable(R.drawable.ic_plane_black))
                        isBusSelected = false
                        isPlaneSelected = false
                    }
                    if (!isTrainSelected) {
                        train_btn.background = mSelectedButtonBg
                        train_btn.setImageDrawable(getDrawable(R.drawable.ic_train_white))
                        isTrainSelected = true
                        radius_seekbar.min = 10
                        radius_seekbar.max = 200
                        radius_seekbar.progress = radius_seekbar.min
                    }
                }
                R.id.plane_btn -> {
                    if (isBusSelected || isTrainSelected) {
                        Groupie(bus_btn,train_btn).setBackground(mUnselectedButtonBg)
                        bus_btn.setImageDrawable(getDrawable(R.drawable.ic_bus_black))
                        train_btn.setImageDrawable(getDrawable(R.drawable.ic_train_black))
                        isBusSelected = false
                        isTrainSelected = false
                    }
                    if (!isPlaneSelected) {
                        plane_btn.background = mSelectedButtonBg
                        plane_btn.setImageDrawable(getDrawable(R.drawable.ic_plane_white))
                        isPlaneSelected = true
                        radius_seekbar.min = 100
                        radius_seekbar.max = 500
                        radius_seekbar.progress = radius_seekbar.min
                    }
                }
            }
        }

        radius_seekbar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar?) {}
            override fun onProgressChanged(seekBar: DiscreteSeekBar?, progress: Int, fromUser: Boolean) {
                mCircle?.remove()
                mCircleOptions.radius(progress * 1000.0)
//                mDisposable=Observable.
                mCircle = mMap?.addCircle(mCircleOptions)
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
                radius_seekbar.isEnabled = true
                mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)))
                mMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
            })
        }

        destination_txt.setOnClickListener {
            destination_txt.setCompletionEnabled(true)
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            if (destination_txt?.text?.isNotEmpty() ?: false) {
                destination_txt.text = null
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
            if (mMarker != null && destination_txt.text.isNotEmpty()) {
                mLocation?.let {
                    //                    if (alarm_sound_check.isChecked && mAlarmVolume <= 0) {
//                        Utils.AlertHelper.snackbar(this, R.string.snackar_no_volume, duration = Snackbar.LENGTH_LONG)
//                    } else
                    if (PreferencesHelper.isAnotherGeofenceActived(this) == true) {
                        Utils.AlertHelper.dialog(this, R.string.dialog_title_another_service, R.string.dialog_message_another_service, {
                            removeGeofence({ addGeofence() })
                        })
                    } else {
                        addGeofence()
                    }
                }
            } else {
                Utils.AlertHelper.snackbar(this, R.string.snackbar_no_location, duration = Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun addGeofence() {
        mGeofenceClient.addGeofences(mGeofenceRequest, mGeoFencePendingIntent)
                .addOnSuccessListener {
                    Utils.AlertHelper.snackbar(this, R.string.snackbar_service_start, actionClick = { finishAndRemoveTask() })
                    PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ADD_GEOFENCE, true)
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

        mMap?.let { setMapUi(it) }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission, actionClick = {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
            })
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
        }
    }
}