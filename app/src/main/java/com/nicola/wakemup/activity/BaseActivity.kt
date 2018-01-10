package com.nicola.wakemup.activity

import android.graphics.Color
import android.os.Bundle
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.AestheticActivity
import com.nicola.wakemup.R
import com.nicola.wakemup.utils.PreferencesHelper
import com.nicola.wakemup.utils.Utils

open class BaseActivity : AestheticActivity() {

    companion object {
        var primaryColor: String? = null
        var accentColor: String? = null
        var isThemeChanged: Boolean? = null
        var isNavBarColor: Boolean? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAesthetic()
    }

    private fun initAesthetic() {
        getColor()

        isThemeChanged = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_THEME, false) as Boolean
        isNavBarColor = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_NAV_BAR_COLOR, false) as Boolean

        Aesthetic.get()
                .activityTheme(if (isThemeChanged == true) {
                    R.style.AppThemeDark
                } else {
                    R.style.AppTheme
                })
                .colorPrimary(Color.parseColor(primaryColor))
                .colorAccent(Color.parseColor(accentColor))
                .colorStatusBarAuto()
                .colorNavigationBar(if (isNavBarColor == true) {
                    Color.parseColor(primaryColor)
                } else {
                    Color.BLACK
                })
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
                .isDark(isThemeChanged == true)
                .apply()
    }


    private fun getColor() {
        val isFirstRun = PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_FIRST_RUN, true) as Boolean
        if (isFirstRun) {
            primaryColor = Utils.getParseColor(Color.parseColor(getString(R.color.red_500)))
            accentColor = Utils.getParseColor(Color.parseColor(getString(R.color.blue_500)))
        } else {
            primaryColor = Utils.getParseColor(this, PreferencesHelper.KEY_PRIMARY_COLOR)
            accentColor = Utils.getParseColor(this, PreferencesHelper.KEY_ACCENT_COLOR)
        }
    }
}