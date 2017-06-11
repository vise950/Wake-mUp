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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_details.*
import nicola.dev.com.allarmap.R
import nicola.dev.com.allarmap.retrofit.MapsGoogleApiClient
import nicola.dev.com.allarmap.service.GeofenceTransitionsIntentService
import nicola.dev.com.allarmap.utils.PreferencesHelper
import nicola.dev.com.allarmap.utils.Utils
import nicola.dev.com.allarmap.utils.log
import nicola.dev.com.allarmap.utils.other
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar


class MainActivity : AppCompatActivity(),
        OnMapReadyCallback,
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

//        Utils.isMyServiceRunning(this,GeofenceTransitionsIntentService::class.java).log("service running")
//        if (Utils.isMyServiceRunning(this, GeofenceTransitionsIntentService::class.java)) {
//            stopService(Intent(this, GeofenceTransitionsIntentService::class.java))
//        }
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
        }

        map?.setOnMapClickListener {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            Utils.hideKeyboard(this)
            mMarker?.remove()
            mCircle?.remove()
            mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)))
            Utils.LocationHelper.getLocationName(it.latitude, it.longitude, {
                destination_txt.setText(it)
                //fixme hide keyboard
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

    override fun onConnectionFailed(result: ConnectionResult) {
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
                //                //todo improve code

            }
        })

        destination_txt.setOnClickListener {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            if (destination_txt?.text?.isNotEmpty() ?: false) {
                destination_txt.text = null
            }
        }

        val resultAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        destination_txt?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            //fixme resultAdapter
            override fun onTextChanged(query: CharSequence, start: Int, before: Int, count: Int) {
                if (!resultAdapter.isEmpty) {
                    resultAdapter.clear()
                }

                if (query.trim() != "" && query.length > 1) {
                    MapsGoogleApiClient.service.getPrediction(query.toString()).subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ data ->
                                if (data?.predictions?.isNotEmpty() ?: false) {
                                    data?.predictions?.forEachIndexed { index, data ->
                                        if (index in 0..3) {
//                                            "add ${data.description.toString()}".log(TAG)
                                            resultAdapter.add(Utils.trimString(data.description.toString()))
                                        }
                                    }
                                } else {
                                    resultAdapter.add(resources.getString(R.string.no_result_suggestion))
                                }
                            }, { error ->
                                error.log("Error call")
                                resultAdapter.add(resources.getString(R.string.error_load_suggestion))
                            })
                }

                destination_txt.setAdapter(resultAdapter)
            }
        })

        destination_txt.setOnItemClickListener { parent, view, position, id ->
            val result = parent.getItemAtPosition(position).toString()
            Utils.hideKeyboard(this)
            Utils.LocationHelper.getCoordinates(result, {
                mMarker?.remove()
                mCircle?.remove()
                mMarker = mMap?.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)))
            })
        }

        fab.setOnClickListener {
            //todo service is running if only my position is inside radius of geofence

            //todo check radious
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            if (mMarker != null) {
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
//        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, mGeofenceRequest, mGeoFencePendingIntent)
//                .setResultCallback {
//                    if (it.isSuccess) {
//                        PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_PREF_SERCVICE, true)
//                        Utils.AlertHepler.snackbar(this, R.string.snackbar_service_start, actionClick = { finish() })
//                    }
//                }

        mGeofenceClient.addGeofences(mGeofenceRequest, mGeoFencePendingIntent)
                .addOnSuccessListener {
                    Utils.AlertHepler.snackbar(this, R.string.snackbar_service_start, actionClick = { finish() })
                    PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_PREF_GEOFENCE, true)
                }
                .addOnFailureListener { it.log(TAG) }
    }

    private fun removeGeofence(callback: (() -> Unit)) {
//        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, mGeoFencePendingIntent)
//                .setResultCallback { if (it.isSuccess) callback.invoke() }

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