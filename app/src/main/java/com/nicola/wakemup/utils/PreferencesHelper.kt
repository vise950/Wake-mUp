package com.nicola.wakemup.utils

import android.content.Context
import android.preference.PreferenceManager
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

    fun setPreferences(context: Context, key: String, value: Any) {
        val sp = context.getSharedPreferences(MainActivity::class.java.name, Context.MODE_PRIVATE)
        val editor = sp.edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Float -> editor.putFloat(key, value)
            is Long -> editor.putLong(key, value)
        }
        editor.apply()
    }

    fun getPreferences(context: Context, key: String, defaultValue: Any): Any? {
        val sp = context.getSharedPreferences(MainActivity::class.java.name, Context.MODE_PRIVATE)
        var value: Any? = null

        when (defaultValue) {
            is String -> value = sp.getString(key, defaultValue)
            is Boolean -> value = sp.getBoolean(key, defaultValue)
            is Int -> value = sp.getInt(key, defaultValue)
            is Float -> value = sp.getFloat(key, defaultValue)
            is Long -> value = sp.getLong(key, defaultValue)
        }

        return value
    }

    fun getDefaultPreferences(context: Context, key: String, defaultValue: Any): Any? {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        var value: Any? = null

        when (defaultValue) {
            is String -> value = sp.getString(key, defaultValue)
            is Boolean -> value = sp.getBoolean(key, defaultValue)
            is Int -> value = sp.getInt(key, defaultValue)
            is Float -> value = sp.getFloat(key, defaultValue)
            is Long -> value = sp.getLong(key, defaultValue)
        }

        return value
    }

    fun isAnotherGeofenceActived(context: Context): Boolean? {
        return getPreferences(context, KEY_ADD_GEOFENCE, false) as? Boolean
    }

    fun isUISystem(context: Context): Boolean? {
        val pref = getDefaultPreferences(context, KEY_DISTANCE, "")
        return pref != "mi"
    }

    fun isFirstRun(context: Context): Boolean? {
        return getPreferences(context, KEY_FIRST_RUN, false) as? Boolean
    }

    fun isShowCase(context: Context): Boolean? {
        return getPreferences(context, KEY_SHOW_CASE, true) as? Boolean
    }
}