package com.nicola.wakemup.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.nicola.wakemup.R
import com.nicola.wakemup.preferences.SettingsActivity
import com.nicola.wakemup.ui.fragment.MapFragment
import com.nicola.wakemup.utils.*

class MainActivity : AppCompatActivity() {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestLocationPermission()

        supportFragmentManager.beginTransaction()
                .add(R.id.map_fragment, MapFragment()).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_LOCATION_CODE) {
            if (permissions.isNotEmpty() && permissions.all { isPermissionGranted(it) })
                retrieveLocation()
            else
                Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission,
                        actionMessage = R.string.action_Ok, actionClick = { requestLocationPermission() })
        }
    }

    private fun requestLocationPermission() {
        if (isPermissionGranted(PERMISSION_FINE_LOCATION) && isPermissionGranted(PERMISSION_BACKGROUND_LOCATION)) {
            retrieveLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(PERMISSION_FINE_LOCATION, PERMISSION_BACKGROUND_LOCATION), PERMISSION_LOCATION_CODE)
        }
    }

    private fun retrieveLocation() {
        fusedLocationClient?.lastLocation?.addOnCompleteListener {
            it.result?.let {
                locationUpdated.invoke(it)
            }
        }
    }
}