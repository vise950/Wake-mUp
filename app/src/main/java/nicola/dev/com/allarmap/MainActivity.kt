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
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_details.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    companion object {
        private val TAG = "ALLARM MAP"
        private val REQUEST_LOCATION = 854
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private val DEFAULT_ZOOM: Float = 8F
        private val ZOOM: Float = 15F
    }

    private var mMap: GoogleMap? = null
    private var mRequestPermissionCount = 0
    private var isLocationPermissionGranted = false

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocation: Location? = null
    private var mLocationName: String? = null
    private var mLocationRequest: LocationRequest? = null

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
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) {
                    destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.color_primary))
                } else {
                    destination_txt.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.color_white))
                }
            }
        })
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