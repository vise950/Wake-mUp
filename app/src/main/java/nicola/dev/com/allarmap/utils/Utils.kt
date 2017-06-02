package nicola.dev.com.allarmap.utils

import android.Manifest
import android.location.Location
import android.location.LocationManager
import com.dev.nicola.allweather.utils.log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import nicola.dev.com.allarmap.retrofit.MapsGoogleApiClient
import android.view.inputmethod.InputMethodManager.HIDE_IMPLICIT_ONLY
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_main.*
import nicola.dev.com.allarmap.R
import javax.xml.datatype.Duration


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

        fun hideKeyboard(context: Context) {
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        }

        fun isPermissionGranted(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        fun requstPermission() {

        }
    }

    object LocationHelper {

        fun getLocationName(latitude: Double, longitude: Double, onSuccess: ((String) -> Unit)? = null, onError: (() -> Unit)? = null) {
            MapsGoogleApiClient.service.getLocationName(latitude.toString() + "," + longitude.toString())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ data ->
                        if (data?.result?.isNotEmpty() ?: false) {
                            data?.result?.get(0)?.addressComponents?.forEach {
                                if (it.types?.get(0) == "locality" || it.types?.get(0) == "administrative_area_level_3") {
                                    onSuccess?.invoke(it.longName.toString())
                                }
                            }
                        }
                    }, { error ->
                        error.log("get location name error")
                        onError?.invoke()
                    })
        }

        fun getCoordinates(cityName: String, onSuccess: ((Location) -> Unit)? = null) {
            MapsGoogleApiClient.service.getCoordinates(cityName).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ data ->
                        if (data?.result?.isNotEmpty() ?: false) {
                            val location = Location(LocationManager.PASSIVE_PROVIDER)
                            location.latitude = data?.result?.get(0)?.geometry?.location?.lat ?: 0.0
                            location.longitude = data?.result?.get(0)?.geometry?.location?.lng ?: 0.0
                            onSuccess?.invoke(location)
                        }
                    }, { error ->
                        error.log("get coordinates error")
                    })
        }
    }

    object SnackBarHepler {

        fun makeSnackbar(activity: Activity, message: Int, view: View = activity.root_container,
                         duration: Int = Snackbar.LENGTH_INDEFINITE,
                         actionMessage: Int = R.string.action_OK,
                         actionClick: (() -> Unit)? = null) {
            val snackBar = Snackbar.make(view, message, duration)
            if (duration == Snackbar.LENGTH_INDEFINITE) {
                snackBar.setAction(actionMessage, { actionClick?.invoke() })
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