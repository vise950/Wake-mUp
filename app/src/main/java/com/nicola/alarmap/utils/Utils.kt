package com.nicola.alarmap.utils

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.nicola.alarmap.retrofit.MapsGoogleApiClient
import com.nicola.com.alarmap.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class Utils {
    companion object {

        fun trimString(s: String): String {
            val index = s.indexOf(',')
            val lastIndex = s.lastIndexOf(',')
            if (index != lastIndex) {
                return s.substring(0, index) + s.substring(lastIndex, s.length)
            }
            return s
        }

        fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            activity.currentFocus?.windowToken?.let {
                imm.hideSoftInputFromWindow(it, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }

        fun isPermissionGranted(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        fun isMyServiceRunning(context: Context,serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE).any { serviceClass.name == it.service.className }
        }

        fun getNowInMls(): Long {
            return System.currentTimeMillis()
        }

        fun getParseColor(context: Context, key: String): String {
            val pref = PreferencesHelper.getDefaultPreferences(context, key, -1) as Int
            return "#${Integer.toHexString(pref).toUpperCase()}"
        }

        fun getParseColor(color: Int): String {
            return "#${Integer.toHexString(color).toUpperCase()}"
        }

        fun vectorToBitmap(context: Context, @DrawableRes id: Int, @ColorInt color: Int?=null): BitmapDescriptor {
            val vectorDrawable = ResourcesCompat.getDrawable(context.resources, id, null)
            val bitmap = Bitmap.createBitmap(vectorDrawable?.intrinsicWidth ?: 0, vectorDrawable?.intrinsicHeight ?: 0, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            vectorDrawable?.setBounds(0, 0, canvas.width, canvas.height)
            vectorDrawable?.let { image ->
                color?.let { c ->
                    DrawableCompat.setTint(image, c)
                }
            }
            vectorDrawable?.draw(canvas)
            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    object LocationHelper {

        private val INVALID_DOUBLE_VALUE = -999.0

        fun getLocationName(latitude: Double, longitude: Double, onSuccess: ((String) -> Unit)? = null) {
            MapsGoogleApiClient.service.getLocationName(latitude.toString() + "," + longitude.toString())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it.results?.isNotEmpty() ?: false) {
                            it.results?.get(0)?.address_components?.forEach {
                                if (it.types?.get(0) == "locality" || it.types?.get(0) == "administrative_area_level_3") {
                                    onSuccess?.invoke(it.long_name.toString())
                                }
                            }
                        } else {
                            onSuccess?.invoke("no valid place")
                        }
                    }, {
                        it.log("get location name error")
                        it.printStackTrace()
                    })
        }

        fun getCoordinates(cityName: String, onSuccess: ((Location) -> Unit)? = null) {
            MapsGoogleApiClient.service.getCoordinates(cityName).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it.results?.isNotEmpty() ?: false) {
                            val location = Location(LocationManager.PASSIVE_PROVIDER)
                            location.latitude = it.results?.get(0)?.geometry?.location?.lat ?: INVALID_DOUBLE_VALUE
                            location.longitude = it.results?.get(0)?.geometry?.location?.lng ?: INVALID_DOUBLE_VALUE
                            onSuccess?.invoke(location)
                        }
                    }, {
                        it.log("get coordinates error")
                        it.printStackTrace()
                    })
        }
    }

    object AlertHelper {

        fun dialog(context: Context, title: Int, message: Int, positiveClick: (() -> Unit)? = null, negativeClick: (() -> Unit)? = null) {
            AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.action_Ok, { dialog, which ->
                        positiveClick?.invoke()
                        dialog.dismiss()
                    })
                    .setNegativeButton(R.string.action_cancel, { dialog, which ->
                        negativeClick?.invoke()
                        dialog.dismiss()
                    })
                    .show()
        }

        fun snackbar(activity: Activity, message: Int, view: View = activity.root_container, duration: Int = Snackbar.LENGTH_LONG) {
            val snackBar = Snackbar.make(view, message, duration)
            showSnackBar(activity, snackBar)
        }

        fun snackbar(activity: Activity, message: Int, view: View = activity.root_container,
                     duration: Int = Snackbar.LENGTH_INDEFINITE,
                     actionMessage: Int,
                     actionClick: (() -> Unit)) {
            val snackBar = Snackbar.make(view, message, duration)
            if (duration == Snackbar.LENGTH_INDEFINITE) {
                snackBar.setAction(actionMessage, { actionClick.invoke() })
            }
            showSnackBar(activity, snackBar)
        }

        private fun showSnackBar(activity: Activity, snackBar: Snackbar) {
            //todo repleace runOnUiThread with anko or koventant
            activity.runOnUiThread {
                snackBar.show()
            }
        }
    }
}