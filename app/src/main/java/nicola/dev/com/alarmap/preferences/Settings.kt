package nicola.dev.com.alarmap.preferences

import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v4.content.res.ResourcesCompat
import android.view.MenuItem
import com.afollestad.aesthetic.Aesthetic
import com.afollestad.aesthetic.AestheticActivity
import com.thebluealliance.spectrum.SpectrumPreference
import nicola.dev.com.alarmap.R
import nicola.dev.com.alarmap.utils.PreferencesHelper
import nicola.dev.com.alarmap.utils.log

class Settings : AestheticActivity() {

    private var primaryColor: Int = 0
    private var accentColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        getColor()
//        if (Aesthetic.isFirstTime()) {
//            Aesthetic.get()
//                    .colorPrimaryRes(primaryColor)
//                    .colorAccentRes(accentColor)
//                    .colorStatusBarAuto()
//                    .textColorPrimaryRes(R.color.color_primary_text_dark)
//                    .textColorSecondaryRes(R.color.color_secondary_text)
//                    .apply()
//        }

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

    fun getColor() {
        primaryColor = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_PRIMARY_COLOR, 0) as Int
        accentColor = PreferencesHelper.getDefaultPreferences(this, PreferencesHelper.KEY_ACCENT_COLOR, 0) as Int
    }


    class AppPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings)

            val primaryColor = findPreference("primary_color") as SpectrumPreference
            primaryColor.color.log("color")
            primaryColor.setOnPreferenceChangeListener { preference, value ->
                value.toString().log("change")
//                val color = ResourcesCompat.getColor(resources, value as Int, null)
                activity.recreate()
                true
            }
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