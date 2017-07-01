package com.nicola.alarmap.preferences

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceFragment
import android.view.MenuItem
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.AestheticActivity
import com.nicola.alarmap.utils.PreferencesHelper
import com.nicola.alarmap.utils.Utils
import com.nicola.com.alarmap.R


class Settings : AestheticActivity() {

    private var mPrimaryColor: String? = null
    private var mAccentColor: String? = null
    private var mThemeChanged: Boolean? = null
    private var mNavBarColor: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        getColor()
        getPreferences()
        Aesthetic.get()
                .activityTheme(if (mThemeChanged == true) {
                    R.style.AppThemeDark
                } else {
                    R.style.AppTheme
                })
                .colorPrimary(Color.parseColor(mPrimaryColor))
                .colorAccent(Color.parseColor(mAccentColor))
                .colorStatusBarAuto()
                .colorNavigationBar(if (mNavBarColor == true) {
                    Color.parseColor(mPrimaryColor)
                } else {
                    Color.BLACK
                })
                .textColorPrimaryRes(if (mThemeChanged == true) {
                    R.color.color_primary_text_inverse
                } else {
                    R.color.color_primary_text
                })
                .textColorSecondaryRes(if (mThemeChanged == true) {
                    R.color.color_secondary_text_inverse
                } else {
                    R.color.color_secondary_text
                })
                .textColorPrimaryInverseRes(R.color.color_primary_text_inverse)
                .isDark(mThemeChanged ?: false)
                .apply()

        fragmentManager.beginTransaction().replace(android.R.id.content, AppPreferenceFragment()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun getPreferences() {
        mThemeChanged = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_THEME, false) as Boolean
        mNavBarColor = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_NAV_BAR_COLOR, false) as Boolean
    }

    private fun getColor() {
        val isFirstRun = PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_FIRST_RUN, true) as Boolean
        if (isFirstRun) {
            mPrimaryColor = Utils.getParseColor(Color.parseColor(getString(R.color.red_500)))
            mAccentColor = Utils.getParseColor(Color.parseColor(getString(R.color.blue_500)))
        } else {
            mPrimaryColor = Utils.getParseColor(this, PreferencesHelper.KEY_PRIMARY_COLOR)
            mAccentColor = Utils.getParseColor(this, PreferencesHelper.KEY_ACCENT_COLOR)
        }
    }


    class AppPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings)

            PreferencesHelper.setPreferences(activity, PreferencesHelper.KEY_FIRST_RUN, false)

            findPreference(getString(R.string.key_theme)).setOnPreferenceChangeListener { preference, value ->
                Handler().postDelayed({
                    activity.recreate()
                }, 300)

//                Completable.complete()
//                        .delay(300, TimeUnit.MILLISECONDS)
//                        .subscribeOn(Schedulers.newThread())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .doOnComplete { activity.recreate() }
//                        .subscribe()
                true
            }

            findPreference(getString(R.string.key_nav_bar_color)).setOnPreferenceChangeListener { preference, value ->
                Handler().postDelayed({
                    activity.recreate()
                }, 400)
                true
            }

            findPreference(getString(R.string.key_primary_color)).setOnPreferenceChangeListener { preference, value ->
                activity.recreate()
                true
            }

            findPreference(getString(R.string.key_accent_color)).setOnPreferenceChangeListener { preference, value ->
                activity.recreate()
                true
            }
        }
    }
}