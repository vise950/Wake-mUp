package com.nicola.wakemup.utils

import android.content.Context
import android.content.SharedPreferences
import com.nicola.wakemup.ui.activity.MainActivity

object PreferencesHelper {

    val KEY_ADD_GEOFENCE: String = "is_geofence_added"
    val KEY_ALARM_SOUND: String = "alarm_sound"
    val KEY_PRIMARY_COLOR: String = "primary_color"
    val KEY_ACCENT_COLOR: String = "accent_color"
    val KEY_THEME: String = "theme"
    val KEY_DISTANCE: String = "distance"
    val KEY_NAV_BAR_COLOR: String = "nav_bar_color"
    val KEY_FIRST_RUN: String = "is_first_run"
    val KEY_SHOW_CASE: String = "show_case"
    val KEY_MAP_STYLE: String = "map_style"

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(MainActivity::class.java.name, Context.MODE_PRIVATE)
    }

    fun setPreferences(key: String, value: Any) {
        val editor = sharedPreferences.edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Float -> editor.putFloat(key, value)
            is Long -> editor.putLong(key, value)
        }
        editor.apply()
    }

    fun getPreferences(key: String, defaultValue: Any): Any? {
        var value: Any? = null

        when (defaultValue) {
            is String -> value = sharedPreferences.getString(key, defaultValue)
            is Boolean -> value = sharedPreferences.getBoolean(key, defaultValue)
            is Int -> value = sharedPreferences.getInt(key, defaultValue)
            is Float -> value = sharedPreferences.getFloat(key, defaultValue)
            is Long -> value = sharedPreferences.getLong(key, defaultValue)
        }

        return value
    }

    fun isAnotherGeofenceActive(): Boolean? =
            getPreferences(KEY_ADD_GEOFENCE, false) as? Boolean

//    fun isISU(context: Context): Boolean? {
//        val pref = getDefaultPreferences(context, KEY_DISTANCE, "")
//        return pref != "mi"
//    }
//
//    fun isShowCase(context: Context): Boolean? =
//            getPreferences(context, KEY_SHOW_CASE, true) as? Boolean
}