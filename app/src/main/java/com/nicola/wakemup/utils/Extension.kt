package com.nicola.wakemup.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import kotlin.reflect.KClass

fun Any?.error(obj: Any? = null) {
    val logger = when (obj) {
        is KClass<*> -> obj.java.simpleName
        is Class<*> -> obj.simpleName
        is String -> obj
        null -> "Default"
        else -> obj.javaClass.simpleName
    }

    val message = when (this) {
        is String? -> this ?: "NullString"
        is Int? -> if (this == null) "NullInt" else "Int: $this"
        is Float? -> if (this == null) "NullFloat" else "Float: $this"
        is Double? -> if (this == null) "NullDouble" else "Double: $this"
        else -> if (this == null) "NullValue" else "Value: $this"
    }
    android.util.Log.e(logger, message)
}

fun Activity.hideKeyboard() {
    (this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
        this@hideKeyboard.currentFocus?.windowToken?.let {
            this.hideSoftInputFromWindow(it, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }
}

fun Context.isPermissionGranted(permission:String):Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED