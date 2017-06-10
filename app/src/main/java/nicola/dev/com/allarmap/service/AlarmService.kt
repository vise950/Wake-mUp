package nicola.dev.com.allarmap.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.IBinder
import android.os.Vibrator
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit


class AlarmService : Service() {

    companion object {
        private val TAG = "ALARM SERVICE"
        private val VIBRATE_DELAY_TIME = 2L
        private val DURATION_OF_VIBRATION = 1500L
    }

    private val mVibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private var mVibrateDisposable: Disposable? = null
    private val mRingtone by lazy { RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) }

    override fun onCreate() {
        super.onCreate()
        startAlarm()
    }

    override fun onDestroy() {
        mVibrateDisposable?.dispose()
        mVibrator.cancel()
        if (mRingtone.isPlaying) {
            mRingtone.stop()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startAlarm() {
        mVibrateDisposable = Observable.interval(VIBRATE_DELAY_TIME, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mVibrator.vibrate(DURATION_OF_VIBRATION)
                })

        val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
        mRingtone.audioAttributes = audioAttr
        mRingtone.play()
    }
}