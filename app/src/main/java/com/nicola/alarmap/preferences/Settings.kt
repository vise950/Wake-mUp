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
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class Settings : AestheticActivity() {

    private var primaryColor: String? = null
    private var accentColor: String? = null
    private var themeChanged: Boolean? = null
    private var navBarColor: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        getColor()
        getPreferences()
        Aesthetic.get()
                .activityTheme(if (themeChanged == true) {
                    R.style.AppThemeDark
                } else {
                    R.style.AppTheme
                })
                .colorPrimary(Color.parseColor(primaryColor))
                .colorAccent(Color.parseColor(accentColor))
                .colorStatusBarAuto()
                .colorNavigationBar(if (navBarColor == true) {
                    Color.parseColor(primaryColor)
                } else {
                    Color.BLACK
                })
                .textColorPrimaryRes(if (themeChanged == true) {
                    R.color.color_primary_text_inverse
                } else {
                    R.color.color_primary_text
                })
                .textColorSecondaryRes(if (themeChanged == true) {
                    R.color.color_secondary_text_inverse
                } else {
                    R.color.color_secondary_text
                })
                .textColorPrimaryInverseRes(R.color.color_primary_text_inverse)
                .isDark(themeChanged ?: false)
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
        themeChanged = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_THEME, false) as Boolean
        navBarColor = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_NAV_BAR_COLOR, false) as Boolean
    }

    private fun getColor() {
        primaryColor = Utils.getParseColor(this, PreferencesHelper.KEY_PRIMARY_COLOR)
        accentColor = Utils.getParseColor(this, PreferencesHelper.KEY_ACCENT_COLOR)
    }


    class AppPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings)

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