package com.nicola.wakemup.utils

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.view.View
import com.nicola.wakemup.R
import kotlinx.android.synthetic.main.activity_main.*


class Utils {
    companion object {

        @Suppress("DEPRECATION")
        fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE).any { serviceClass.name == it.service.className }
        }

        fun getNowInMls(): Long = System.currentTimeMillis()

        fun getParseColor(color: Int): String = "#${Integer.toHexString(color).toUpperCase()}"

        fun milesToMeters(miles: Int): Double = (miles * 1609.344)

        fun isLocationGranted(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
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