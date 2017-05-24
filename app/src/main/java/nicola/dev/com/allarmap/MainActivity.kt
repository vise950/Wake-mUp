package nicola.dev.com.allarmap

import android.Manifest
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
import com.dev.nicola.allweather.utils.log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_details.*
import nicola.dev.com.allarmap.retrofit.MapsGoogleApiClient
import nicola.dev.com.allarmap.utils.Utils


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    companion object {
        private val TAG = "ALLARM MAP"
        private val REQUEST_LOCATION = 854
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private val DEFAULT_ZOOM: Float = 8F
        private val ZOOM: Float = 10F
    }

    private var mMap: GoogleMap? = null
    private var circle: Circle? = null
    private var mRequestPermissionCount = 0
    private var isLocationPermissionGranted = false

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocation: Location? = null
    private var mLocationName: String? = null
    private var mLocationRequest: LocationRequest? = null
    private var mMarker: Marker? = null

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
//        initUi()
    }

    override fun onResume() {
        super.onResume()
        if (mGoogleApiClient?.isConnected ?: false) {
            getDeviceLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mGoogleApiClient?.isConnected ?: false) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (mGoogleApiClient?.isConnected ?: false) {
            mGoogleApiClient?.disconnect()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            this.mMap = it
            setMapUi(it)
        }

        map?.setOnMapClickListener {
            mBottomSheetBehavior?.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
    }

    override fun onConnected(bundle: Bundle?) {
        getDeviceLocation()
        initMap()
    }

    override fun onConnectionSuspended(i: Int) {
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
                    init()
                } else {
                    if (mRequestPermissionCount < 2 && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Snackbar.make(root_container, "I needed location permission for determinate your position.", Snackbar.LENGTH_INDEFINITE)
                                .setAction("OK", {
                                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
                                })
                                .show()
                        mRequestPermissionCount++
                    } else {
                        Snackbar.make(root_container, "Go to app setting and enable permission.", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun init() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleClient()
            mGoogleApiClient?.connect()
            initUi()
        } else
            requestLocationPermission()
    }

    private fun initMap() {
        if (mMap == null) {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
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

        radius_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                radius_txt.text = progress.toString()
                val circleOptions = CircleOptions()
                        .center(LatLng(mMarker?.position?.latitude ?: 100.0, mMarker?.position?.longitude ?: 100.0))
                        .radius(1000.0 * progress)
                        .strokeWidth(10F)
                        .strokeColor(ContextCompat.getColor(this@MainActivity, R.color.stoke_map))
                        .fillColor(ContextCompat.getColor(this@MainActivity, R.color.fill_map))

                circle?.remove()
                circle = mMap?.addCircle(circleOptions)

                //todo improve code
            }
        })

        destination_txt.setOnClickListener {
            mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val resultAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        destination_txt?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                destination_txt?.setAdapter(resultAdapter)
            }

            //fixme resultAdapter
            override fun onTextChanged(query: CharSequence, start: Int, before: Int, count: Int) {
                if (query.trim() != "" && query.length > 1) {
                    MapsGoogleApiClient.service.getPrediction(query.toString()).subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ data ->
                                if (data?.predictions?.isNotEmpty() ?: false) {
                                    data?.predictions?.forEachIndexed { index, data ->
                                        if (index in 0..3) {
                                            resultAdapter.add(Utils.trimString(data.description.toString()))
                                        }
                                    }
                                } else {
                                    resultAdapter.clear()
                                    resultAdapter.add(resources.getString(R.string.no_result_suggestion))
                                }
                            }, { error ->
                                error.log("Error call")
                                resultAdapter.add(resources.getString(R.string.error_load_suggestion))
                            })
                }
            }
        })

        destination_txt.setOnItemClickListener { parent, view, position, id ->
            val result = parent.getItemAtPosition(position).toString()
            Utils.LocationHelper.getCoordinates(result, {
                mMarker = mMap?.addMarker(MarkerOptions()
                        .position(LatLng(it.latitude, it.longitude)))
            })
        }
    }

    private fun setMapUi(map: GoogleMap) {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
        mLocation?.let { map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_ZOOM)) }
        map.animateCamera(CameraUpdateFactory.zoomTo(ZOOM), 2000, null)
    }


    @Synchronized private fun buildGoogleClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build()
        createLocationRequest()
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest?.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest?.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun getDeviceLocation() {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(root_container, "I needed location permission for determinate your position.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", {
                        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
                    })
                    .show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
        }
    }
}