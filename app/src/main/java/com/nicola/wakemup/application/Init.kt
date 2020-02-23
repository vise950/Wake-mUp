package com.nicola.wakemup.application

import android.app.Application
import com.nicola.wakemup.utils.AlarmHelper
import com.nicola.wakemup.utils.GeofenceHelper
import com.nicola.wakemup.utils.NotificationHelper
import com.nicola.wakemup.utils.PreferencesHelper

class Init : Application() {

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.init(this)
        AlarmHelper.init(this)
        PreferencesHelper.init(this)
        GeofenceHelper.init(this)
    }
}