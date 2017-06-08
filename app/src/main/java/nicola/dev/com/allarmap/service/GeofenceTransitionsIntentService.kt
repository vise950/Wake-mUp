package nicola.dev.com.allarmap.service

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.*
import android.os.Vibrator
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import com.dev.nicola.allweather.utils.log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import nicola.dev.com.allarmap.R
import nicola.dev.com.allarmap.ui.activity.MainActivity
import nicola.dev.com.allarmap.utils.Utils
import java.util.concurrent.TimeUnit


class GeofenceTransitionsIntentService : IntentService(TAG) {

    companion object {
        private val TAG = "GeofenceTransitionsIntentService"
        private val VIBRATE_DELAY_TIME = 2000L
        private val DURATION_OF_VIBRATION = 1500L
        private val VOLUME_INCREASE_DELAY = 600
        private val VOLUME_INCREASE_STEP = 0.01f
        private val MAX_VOLUME = 1.0f
    }

    private val mNotificationIntent by lazy { Intent(this, MainActivity::class.java) }
    private var mStackBuilder: TaskStackBuilder? = null
    private val mNotificationPendingIntent by lazy { mStackBuilder?.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT) }
    private val mIntentStopAlarm by lazy {
        val stopAlarm = Intent()
        stopAlarm.action = "STOP_ALARM"
        PendingIntent.getBroadcast(this, 2, stopAlarm, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private val mNotification by lazy {
        NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Notifications Example")
                .setContentText("This is a test notification")
                .setWhen(Utils.getNowInMls())
                .setAutoCancel(true)
                .addAction(R.drawable.ic_alarm_off, "Stop", mIntentStopAlarm)
                .setContentIntent(mNotificationPendingIntent)
    }
    private val mNotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val mVibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private var mVibrateDisposable: Disposable? = null
    private var mPlayer: MediaPlayer? = null
    private var mVolumeLevel = 1f
    private var mVolumeDisposable: Disposable? = null


    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            getErrorString(geofencingEvent.errorCode).log(TAG)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            createNotification()
            startAlarm()
        }
    }

    fun stopService() {
        "stop my service".log(TAG)
        mVibrateDisposable?.dispose()
//        mVibrator.cancel()
        mVolumeDisposable?.dispose()
        mPlayer?.stop()
        mPlayer?.release()
    }

    private fun createNotification() {
        mStackBuilder = TaskStackBuilder.create(this)
        mStackBuilder?.addParentStack(MainActivity::class.java)
        mStackBuilder?.addNextIntent(mNotificationIntent)

        mNotificationManager.notify(999, mNotification.build())
    }

    private fun startAlarm() {
        mVibrateDisposable = Observable.interval(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mVibrator.vibrate(1500L)
                })

        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mPlayer = MediaPlayer.create(this, ringtone)
        mPlayer?.setOnErrorListener { mp, what, extra ->
            mp.stop()
            mp.release()
            return@setOnErrorListener true
        }
        mPlayer?.isLooping = true
//        mPlayer?.setVolume(mVolumeLevel, mVolumeLevel)
//        mPlayer?.prepare()
        mPlayer?.start()
//        mVolumeDisposable = Observable.interval(4, TimeUnit.SECONDS)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe({
//                    mPlayer?.let {
//                        if (mVolumeLevel < MAX_VOLUME) {
//                            mVolumeLevel.log("volume")
//                            mVolumeLevel += VOLUME_INCREASE_STEP
//                            mPlayer?.setVolume(mVolumeLevel, mVolumeLevel)
//                        }
//                    }
//                })
    }

    // Handle errors
    private fun getErrorString(errorCode: Int): String {
        when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> return "GeoFence not available"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> return "Too many GeoFences"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> return "Too many pending intents"
            else -> return "Unknown error."
        }
    }
}