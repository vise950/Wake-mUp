package com.nicola.wakemup.application

import android.app.Application
import com.nicola.wakemup.utils.AlarmHelper
import com.nicola.wakemup.utils.GeofenceHelper
import com.nicola.wakemup.utils.NotificationHelper
import com.nicola.wakemup.utils.PreferencesHelper

class Init : Application() {

    override fun onCreate() {
        super.onCreate()

        AlarmHelper.init(this)
        NotificationHelper.init(this)
        PreferencesHelper.init(this)
        GeofenceHelper.init(this)
    }
}