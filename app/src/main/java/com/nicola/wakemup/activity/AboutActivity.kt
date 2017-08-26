package com.nicola.wakemup.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.view.MenuItem
import com.nicola.wakemup.R
import kotlinx.android.synthetic.main.activity_about.*


class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.action_menu_about)

        app_version_tv.text = String.format(resources.getString(R.string.app_version), packageManager.getPackageInfo(packageName, 0).versionName)
        contact_support_cv.setOnClickListener { contactSupport() }
        rate_tv.setOnClickListener { rate() }
        share_tv.setOnClickListener { share() }
        linkedin_tv.setOnClickListener { linkedin() }
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

    private fun contactSupport() {
        val supportEmail = "nicoladev95@gmail.com"
        val subject = "WAKE MUP SUPPORT"
        val body = "Manufacturer: ${android.os.Build.MANUFACTURER}\n" +
                "Model: ${android.os.Build.MODEL}\n" +
                "Device: ${android.os.Build.DEVICE}\n" +
                "Api Version: ${android.os.Build.VERSION.SDK_INT}\n" +
                "OS Version: ${android.os.Build.VERSION.RELEASE}\n" +
                "App Version: ${packageManager.getPackageInfo(packageName, 0).versionName}\n\n" +
                "Describe problem (italian or english): "

        val intent = Intent(Intent.ACTION_VIEW)
        val data = Uri.parse("mailto:$supportEmail?subject=$subject&body=$body")
        intent.data = data
        startActivity(intent)
    }

    private fun rate() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=com.nicola.wakemup")
        startActivity(intent)
    }

    private fun share() {
        ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText("Hey check out this app at: https://play.google.com/store/apps/details?id=com.dev.nicola.allweather")
                .startChooser()
    }

    private fun linkedin() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/davide-ghirelli-14a5a2133/"))
        startActivity(browserIntent)
    }
}