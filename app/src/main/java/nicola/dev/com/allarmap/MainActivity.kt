package nicola.dev.com.allarmap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private val TAG = MainActivity::class.java.canonicalName
        private val REQUEST_LOCATION = 854
    }

    private var mMap: GoogleMap? = null
    private var mRequestPermissionCount = 0
    private var mPermissionAskAgain=true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initMap()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            this.mMap = it
            setMapUi(it)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initMap()
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

    private fun initMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
        } else {
            if (mMap == null) {
                val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }
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

    private fun setMapUi(map: GoogleMap) {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
    }


}