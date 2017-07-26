package com.nicola.alarmap.ui.activity

import android.graphics.Color
import android.os.Bundle
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.AestheticActivity
import com.nicola.alarmap.utils.PreferencesHelper
import com.nicola.alarmap.utils.Utils
import com.nicola.com.alarmap.R

open class BaseActivity : AestheticActivity() {

    companion object {
        var mPrimaryColor: String? = null
        var mAccentColor: String? = null
        var isThemeChanged: Boolean? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAesthetic()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        getColor()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initAesthetic() {
        getColor()
        isThemeChanged = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_THEME, false) as Boolean
        Aesthetic.get()
                .activityTheme(if (isThemeChanged == true) {
                    R.style.AppThemeDark
                } else {
                    R.style.AppTheme
                })
                .colorPrimary(Color.parseColor(mPrimaryColor))
                .colorAccent(Color.parseColor(mAccentColor))
                .colorStatusBarAuto()
                .textColorPrimaryRes(if (isThemeChanged == true) {
                    R.color.color_primary_text_inverse
                } else {
                    R.color.color_primary_text
                })
                .textColorSecondaryRes(if (isThemeChanged == true) {
                    R.color.color_secondary_text_inverse
                } else {
                    R.color.color_secondary_text
                })
                .textColorPrimaryInverseRes(R.color.color_primary_text_inverse)
                .isDark(isThemeChanged ?: false)
                .apply()
    }

    private fun getColor(){
        val isFirstRun = PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_FIRST_RUN, true) as Boolean
        if (isFirstRun) {
            mPrimaryColor = Utils.getParseColor(Color.parseColor(getString(R.color.red_500)))
            mAccentColor = Utils.getParseColor(Color.parseColor(getString(R.color.blue_500)))
        } else {
            mPrimaryColor = Utils.getParseColor(this, PreferencesHelper.KEY_PRIMARY_COLOR)
            mAccentColor = Utils.getParseColor(this, PreferencesHelper.KEY_ACCENT_COLOR)
        }
    }
}