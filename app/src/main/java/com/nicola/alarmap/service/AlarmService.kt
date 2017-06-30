package com.nicola.alarmap.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.IBinder
import android.os.Vibrator
import com.nicola.alarmap.utils.PreferencesHelper

class AlarmService : Service() {

    companion object {
        private val TAG = "ALARM SERVICE"
        private val DELAY_OF_VIBRATION = 1000L
        private val DURATION_OF_VIBRATION = 1500L
    }

    private val mVibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private val mRingtone by lazy { RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) }
    private val mAudioAttr by lazy { AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build() }
    private val isAlarmSound by lazy { PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, true) as Boolean }

    override fun onCreate() {
        super.onCreate()
        startAlarm()
    }

    override fun onDestroy() {
        mVibrator.cancel()
        if (mRingtone.isPlaying) {
            mRingtone.stop()
        }
        PreferencesHelper.setPreferences(this, PreferencesHelper.KEY_ADD_GEOFENCE, false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @Suppress("DEPRECATION")
    private fun startAlarm() {
        mVibrator.vibrate(longArrayOf(DELAY_OF_VIBRATION, DURATION_OF_VIBRATION), 0)
        if (isAlarmSound) {
            mRingtone.audioAttributes = mAudioAttr
            mRingtone.play()
        }
    }
}