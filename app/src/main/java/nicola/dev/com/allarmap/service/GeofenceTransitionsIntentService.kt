package nicola.dev.com.allarmap.service

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.Vibrator
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import com.dev.nicola.allweather.utils.log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import nicola.dev.com.allarmap.R
import nicola.dev.com.allarmap.ui.activity.MainActivity
import nicola.dev.com.allarmap.utils.Utils


class GeofenceTransitionsIntentService : IntentService(TAG) {

    companion object {
        private val TAG = "GeofenceTransitionsIntentService"
        private val VIBRATE_DELAY_TIME = 2000L
        private val DURATION_OF_VIBRATION = 1500L
        private val VOLUME_INCREASE_DELAY = 600
        private val VOLUME_INCREASE_STEP = 0.01f
        private val MAX_VOLUME = 1.0f
    }

    private var mVibrator: Vibrator? = null
    private val mPlayer: MediaPlayer? = null
    private val mVolumeLevel = 0f

    private val mHandler = Handler()

    private val mVibrationLoop by lazy {
        mVibrator?.vibrate(longArrayOf(VIBRATE_DELAY_TIME, DURATION_OF_VIBRATION, VIBRATE_DELAY_TIME), 1)
    }

    

    /*

    private Runnable mVibrationRunnable = new Runnable() {
        @Override
        public void run() {
            mVibrator.vibrate(DURATION_OF_VIBRATION);
            // Provide loop for vibration
            mHandler.postDelayed(mVibrationRunnable,
                    DURATION_OF_VIBRATION + VIBRATE_DELAY_TIME);
        }
    };

    private Runnable mVolumeRunnable = new Runnable() {
        @Override
        public void run() {
            // increase volume level until reach max value
            if (mPlayer != null && mVolumeLevel < MAX_VOLUME) {
                mVolumeLevel += VOLUME_INCREASE_STEP;
                mPlayer.setVolume(mVolumeLevel, mVolumeLevel);
                // set next increase in 600ms
                mHandler.postDelayed(mVolumeRunnable, VOLUME_INCREASE_DELAY);
            }
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = (mp, what, extra) -> {
        mp.stop();
        mp.release();
        mHandler.removeCallbacksAndMessages(null);
        AlarmService.this.stopSelf();
        return true;
    };


     */

    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            getErrorString(geofencingEvent.errorCode).log(TAG)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            startAlert()
            createNotification()
        }
    }

    override fun stopService(name: Intent?): Boolean {
        mVibrator?.cancel()
        return super.stopService(name)
    }

    private fun createNotification() {
        "notification".log(TAG)
        val notificationIntent = Intent(this, MainActivity::class.java)

        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(notificationIntent)

        val notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
//        val contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//        builder.setContentIntent(contentIntent)

        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Notifications Example")
                .setContentText("This is a test notification")
                .setWhen(Utils.getNowInMls())
                .setAutoCancel(true)
                .setContentIntent(notificationPendingIntent)
//                .addAction()


        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
    }

    fun startAlert() {
        "alert".log(TAG)
        mVibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        //start with 2 sec of delay
        //vibrate for 1.5 sec
        //sleep for 1 sec
        val pattern = longArrayOf(2000, 1500, 1000)

        // '0' here means to repeat indefinitely
        // '0' is actually the index at which the pattern keeps repeating from (the start)
        // To repeat the pattern from any other point, you could increase the index, e.g. '1'
        // '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array

        mVibrator?.vibrate(pattern, 1)
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