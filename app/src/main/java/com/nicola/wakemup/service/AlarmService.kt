package com.nicola.wakemup.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import com.nicola.wakemup.utils.PreferencesHelper
import com.nicola.wakemup.utils.error

class AlarmService : Service() {

    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private val vibratePattern by lazy { longArrayOf(0, 1500, 800) }
    private val amplitudes by lazy { intArrayOf(0, 255, 0) }
    private val ringtone by lazy { RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) }
    private val audioAttr by lazy { AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build() }
    private val isAlarmSound by lazy { PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, true) as Boolean }

    override fun onCreate() {
        super.onCreate()
        if (PreferencesHelper.isAnotherGeofenceActive(this) == true) {
            startAlarm()
        }
    }

    override fun onDestroy() {
        vibrator.cancel()
        if (ringtone?.isPlaying == true) {
            ringtone?.stop()
        }
        PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ADD_GEOFENCE, false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    @Suppress("DEPRECATION")
    private fun startAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(vibratePattern, amplitudes, 0))
        } else {
            vibrator.vibrate(vibratePattern, 0)
        }

        if (isAlarmSound) {
            ringtone?.audioAttributes = audioAttr
            ringtone?.play()
        }
    }
}