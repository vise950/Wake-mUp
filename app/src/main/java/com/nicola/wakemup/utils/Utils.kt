package com.nicola.wakemup.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.nicola.wakemup.R
import com.nicola.wakemup.retrofit.MapsGoogleApiClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class Utils {
    companion object {

        @Suppress("DEPRECATION")
        fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE).any { serviceClass.name == it.service.className }
        }

        fun getNowInMls(): Long = System.currentTimeMillis()

        fun getParseColor(context: Context, key: String): String {
            val pref = PreferencesHelper.getDefaultPreferences(context, key, -1) as Int
            return "#${Integer.toHexString(pref).toUpperCase()}"
        }

        fun getParseColor(color: Int): String = "#${Integer.toHexString(color).toUpperCase()}"

        fun milesToMeters(miles: Int): Double = (miles * 1609.344)
    }

    object PermissionHelper {
        fun gotoSetting(activity: Activity) =
                AlertHelper.snackbar(activity, R.string.snackbar_permission_denied,
                        actionMessage = R.string.action_Ok, actionClick = {
                    val intent = Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", activity.packageName, null))
                    activity.startActivity(intent)
                })
    }

    object LocationHelper {

        fun getLocationName(context: Context, latitude: Double, longitude: Double, onSuccess: ((String) -> Unit)? = null) {
            MapsGoogleApiClient.service.getLocationName(latitude.toString() + "," + longitude.toString())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it.results?.isNotEmpty() == true) {
                            it.results[0].address_components?.forEach {
                                if (it.types?.get(0) == "locality" || it.types?.get(0) == "administrative_area_level_3") {
                                    onSuccess?.invoke(it.long_name.toString())
                                    return@subscribe
                                } else {
                                    onSuccess?.invoke(context.getString(R.string.unknown_place))
                                }
                            }
                        } else {
                            onSuccess?.invoke(context.getString(R.string.unknown_place))
                        }
                    }, {
                        it.error("get location name error")
                        it.printStackTrace()
                        AlertHelper.dialog(context, R.string.dialog_message_network_problem, {})
                    })
        }
    }

    object AlertHelper {

        fun dialog(context: Context, message: Int, positiveClick: (() -> Unit)? = null) {
            AlertDialog.Builder(context)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.action_Ok, { dialog, _ ->
                        positiveClick?.invoke()
                        dialog.dismiss()
                    })
                    .show()
        }

        fun dialog(context: Context, title: Int, message: Int, positiveClick: (() -> Unit)? = null, negativeClick: (() -> Unit)? = null) {
            AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.action_Ok, { dialog, _ ->
                        positiveClick?.invoke()
                        dialog.dismiss()
                    })
                    .setNegativeButton(R.string.action_cancel, { dialog, _ ->
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

        private fun showSnackBar(activity: Activity, snackBar: Snackbar) = activity.runOnUiThread {
            snackBar.show()
        }
    }
}