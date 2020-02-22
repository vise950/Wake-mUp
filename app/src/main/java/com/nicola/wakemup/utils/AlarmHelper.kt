package com.nicola.wakemup.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.nicola.wakemup.service.AlarmBroadcastReceiver

object AlarmHelper {

    private lateinit var vibrator: Vibrator
    private lateinit var ringtone: Ringtone
    private val vibratePattern by lazy { longArrayOf(0, 1500, 800) }
    private val amplitudes by lazy { intArrayOf(0, 255, 0) }
    private val audioAttr by lazy { AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build() }
//    private val isAlarmSound by lazy { PreferencesHelper.getPreferences(this, PreferencesHelper.KEY_ALARM_SOUND, true) as Boolean }

    private lateinit var alarmIntent: Intent
    private lateinit var alarmPendingIntent: PendingIntent

    fun init(context: Context) {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        alarmIntent = Intent(context, AlarmBroadcastReceiver::class.java)
        alarmPendingIntent = PendingIntent.getBroadcast(context, 0, getStopIntent(), 0)
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createWaveform(vibratePattern, amplitudes, 0))
        else
            vibrator.vibrate(vibratePattern, 0)

//        if (isAlarmSound) {
        ringtone.audioAttributes = audioAttr
        ringtone.play()
//        }
    }

    fun stop() {
        vibrator.cancel()
        if (ringtone.isPlaying)
            ringtone.stop()
    }

    fun getStartIntent(): Intent {
        alarmIntent.action = START_ALARM
        return alarmIntent
    }

    private fun getStopIntent(): Intent {
        alarmIntent.action = STOP_ALARM
        return alarmIntent
    }

    fun getStopPendingIntent() = alarmPendingIntent
}