package com.nicola.alarmap.utils

import android.content.Context
import android.preference.PreferenceManager
import com.nicola.alarmap.ui.activity.MainActivity

object PreferencesHelper {

    val KEY_ADD_GEOFENCE: String = "is_geofence_added"
    val KEY_ALARM_SOUND: String = "alarm_sound"
    val KEY_PRIMARY_COLOR:String= "primary_color"
    val KEY_ACCENT_COLOR:String= "accent_color"
    val KEY_THEME: String = "theme"
    val KEY_NAV_BAR_COLOR: String = "nav_bar_color"
    val KEY_FIRST_RUN: String = "is_first_run"

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
}