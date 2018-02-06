package protect.babysleepsounds;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;

public class AudioService extends Service
{
    private final static String TAG = "BabySleepSounds";

    private static final int NOTIFICATION_ID = 1;

    public static final String AUDIO_FILENAME_ARG = "AUDIO_FILENAME_ARG";

    private LoopingAudioPlayer _mediaPlayer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        // Used only in case of bound services.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        String audioFilename = intent.getStringExtra(AUDIO_FILENAME_ARG);
        if(audioFilename != null)
        {
            Log.i(TAG, "Received intent to start playback");
            if(_mediaPlayer != null)
            {
                _mediaPlayer.stop();
            }
            _mediaPlayer = new LoopingAudioPlayer(this, new File(audioFilename));
            _mediaPlayer.start();

            setNotification();
        }
        else
        {
            Log.i(TAG, "Received intent to stop playback");

            if(_mediaPlayer != null)
            {
                _mediaPlayer.stop();
                _mediaPlayer = null;
            }

            stopForeground(true);
            stopSelf();
        }

        // If this service is killed, let is remain dead until explicitly started again.
        return START_NOT_STICKY;
    }

    private void setNotification()
    {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.playing_notification)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notificationPlaying));

        // Creates an explicit intent for the Activity
        Intent resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                resultIntent, 0);
        builder.setContentIntent(resultPendingIntent);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy()
    {
        if(_mediaPlayer != null)
        {
            _mediaPlayer.stop();
        }
    }
}
