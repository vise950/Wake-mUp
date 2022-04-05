package com.nicola.wakemup.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.nicola.wakemup.R
import com.nicola.wakemup.databinding.ActivityMainBinding
import com.nicola.wakemup.ui.fragment.MapFragment
import com.nicola.wakemup.utils.hasPermission
import com.nicola.wakemup.utils.locationUpdated


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
//        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.e("DEBUG", "${it.key} = ${it.value}")
//                if (!it.value) {
//                    Utils.AlertHelper.snackbar(this, R.string.snackbar_ask_permission,
//                        actionMessage = R.string.action_Ok, actionClick = {
//                            askPermissions(this.permissions)
//                        })
//                    return@registerForActivityResult
//                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        askPermissions(permissions)

        supportFragmentManager.beginTransaction()
            .add(binding.mapFragment.id, MapFragment()).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun askPermissions(permissions: Array<String>) {
        if (!hasPermission(permissions)) {
            requestMultiplePermissions.launch(permissions)
        } else {
            Log.d("PERMISSIONS", "All permissions are already granted")
            retrieveLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun retrieveLocation() {
        fusedLocationClient?.lastLocation?.addOnCompleteListener {
            it.result?.let {
                locationUpdated.invoke(it)
            }
        }
    }
}