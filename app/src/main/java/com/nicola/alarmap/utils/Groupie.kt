package com.nicola.alarmap.utils

import android.view.View

class Groupie<T : View>(vararg views: T) {

    val mViews = ArrayList(views.asList())

    var visibility: Int = View.VISIBLE
        set(value) {
            field = value
            if (value == View.VISIBLE || value == View.INVISIBLE || value == View.GONE) {
                mViews.forEach { it.visibility = value }
            }
        }

    var isFocusable: Boolean = true
        set(value) {
            field = value
            mViews.forEach { it.isFocusable = value; it.isFocusableInTouchMode = value }
        }

    var isClickable: Boolean = true
        set(value) {
            field = value
            mViews.forEach { it.isClickable = value }
        }

    fun setOnClickListener(listener: ((View) -> Unit)?) {
        mViews.forEach { it.setOnClickListener { listener?.invoke(it) } }
    }
}