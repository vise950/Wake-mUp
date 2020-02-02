package com.nicola.wakemup.service

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import com.nicola.wakemup.utils.RESULT_DATA_KEY


class AddressReceiver internal constructor(handler: Handler? = null)
    : ResultReceiver(handler) {

    var onResultReceive: ((String?) -> Unit)? = null

    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
        resultData.getString(RESULT_DATA_KEY)?.let {
            onResultReceive?.invoke(it)
        }
    }
}