package nicola.dev.com.alarmap.preferences

import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import nicola.dev.com.alarmap.R

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

//
//            val provider = findPreference(PreferencesHelper.KEY_PREF_WEATHER_PROVIDER) as ListPreference
//            provider.setOnPreferenceChangeListener { preference, value ->
//                if (value == WeatherProvider.YAHOO.value) {
//                    SnackBarHelper.yahooProvider(activity, view)
//                }
//                true
//            }
//            provider.isEnabled = isProVersion
//
//            val theme = findPreference(PreferencesHelper.KEY_PREF_THEME)
//            theme.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, value ->
//                // entra se clicco su un item del dialog (anche se è lo stesso) quindi controllo se il value nuovo e diverso da quello vecchio
//                if (PreferencesHelper.isPreferenceChange(activity, PreferencesHelper.KEY_PREF_THEME, PreferencesHelper.KEY_PREF_THEME, value.toString()) ?: false) {
//                    activity.recreate()
//                }
//                true
//            }
        }
    }

}