package com.nicola.wakemup.preferences

import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceFragment
import android.view.MenuItem
import com.nicola.wakemup.R
import com.nicola.wakemup.activity.BaseActivity
import com.nicola.wakemup.utils.PreferencesHelper

class Settings : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.action_menu_settings)

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

    class AppPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings)

            PreferencesHelper.setPreferences(activity, PreferencesHelper.KEY_FIRST_RUN, false)

            findPreference(getString(R.string.key_theme)).setOnPreferenceChangeListener { preference, value ->
                Handler().postDelayed({
                    activity.recreate()
                }, 300)
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