package com.nicola.alarmap.preferences

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.MenuItem
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.AestheticActivity
import com.nicola.com.alarmap.R
import com.nicola.alarmap.utils.PreferencesHelper
import com.nicola.alarmap.utils.Utils

class Settings : AestheticActivity() {

    private var primaryColor: String? = null
    private var accentColor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        getColor()
        Aesthetic.get()
                .colorPrimary(Color.parseColor(primaryColor))
                .colorAccent(Color.parseColor(accentColor))
                .colorStatusBarAuto()
                .textColorPrimaryRes(R.color.color_primary_text_dark)
                .textColorSecondaryRes(R.color.color_secondary_text)
                .isDark(false)
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

    private fun getColor() {
        primaryColor = Utils.getParseColor(this, PreferencesHelper.KEY_PRIMARY_COLOR)
        accentColor = Utils.getParseColor(this, PreferencesHelper.KEY_ACCENT_COLOR)
    }


    class AppPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings)

            findPreference("primary_color").setOnPreferenceChangeListener { preference, value ->
                activity.recreate()
                true
            }

            findPreference("accent_color").setOnPreferenceChangeListener { preference, value ->
                activity.recreate()
                true
            }
        }
    }

}