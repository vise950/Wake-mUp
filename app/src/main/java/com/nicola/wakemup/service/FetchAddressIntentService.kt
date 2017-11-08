package com.nicola.wakemup.service

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.ResultReceiver
import com.google.android.gms.maps.model.LatLng
import com.nicola.wakemup.R
import com.nicola.wakemup.utils.Constants
import com.nicola.wakemup.utils.error
import java.io.IOException
import java.util.*

class FetchAddressIntentService : IntentService(TAG) {

    companion object {
        private val TAG = "FetchAddressIntentService"
    }

    private var receiver: ResultReceiver? = null
    private var location: LatLng? = null
    private val geocoder by lazy { Geocoder(this, Locale.getDefault()) }
    private var addresses: List<Address>? = null

    override fun onHandleIntent(intent: Intent?) {
        receiver = intent?.getParcelableExtra(Constants.RECEIVER)
        location = intent?.getParcelableExtra(Constants.LOCATION_DATA_EXTRA)

        try {
            addresses = geocoder.getFromLocation(location?.latitude ?: Constants.INVALID_DOUBLE_VALUE,
                    location?.longitude ?: Constants.INVALID_DOUBLE_VALUE, 1)
        } catch (ioException: IOException) {
            ioException.error()
        } catch (illegalArgumentException: IllegalArgumentException) {
            illegalArgumentException.error()
        }

        if (addresses?.isNotEmpty() == true) {
            addresses?.first()?.locality?.let { deliverResultToReceiver(it) }
        } else {
            deliverResultToReceiver(getString(R.string.no_result_suggestion))
        }
    }

    private fun deliverResultToReceiver(message: String) {
        receiver?.send(Constants.FETCH_ADDRESS, Bundle().apply { putString(Constants.RESULT_DATA_KEY, message) })
    }
}