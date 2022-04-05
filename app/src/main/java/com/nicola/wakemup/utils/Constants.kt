package com.nicola.wakemup.utils

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi


private const val PACKAGE_NAME = "com.nicola.wakemup"

const val PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
const val PERMISSION_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
@RequiresApi(Build.VERSION_CODES.Q)
const val PERMISSION_BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
const val PERMISSION_LOCATION_CODE = 372

const val GEOFENCE_ID = "WakemUp_geofence"
const val GEOFENCE_EXPIRE_DURATION = 2 * 60 * 60 * 1000L //2h in millisecond
const val GEOFENCE_INTENT_SERVICE_JOB_ID = 852

const val NOTIFICATION_ID = 999
const val NOTIFICATION_CHANNEL_ID = "wakemup_notification" //todo rename

const val DEFAULT_ZOOM: Float = 0F
const val ZOOM: Float = 10F

const val START_ALARM = "START_ALARM"
const val STOP_ALARM = "STOP_ALARM"


val LOCATION_SETTINGS = 555

val INVALID_FLOAT_VALUE = -999.0F
val INVALID_DOUBLE_VALUE = -999.0

val FETCH_ADDRESS = 364
val RECEIVER = PACKAGE_NAME + ".RECEIVER"
val RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY"
val LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA"

