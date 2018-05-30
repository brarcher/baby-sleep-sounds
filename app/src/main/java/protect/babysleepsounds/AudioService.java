package protect.babysleepsounds;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;

public class AudioService extends Service
{
    private final static String TAG = "BabySleepSounds";

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = TAG;

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
        String channelId = "";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            channelId = createNotificationChannel();
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, channelId)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel()
    {
        String channelId = NOTIFICATION_CHANNEL_ID;
        String channelName = getString(R.string.notificationChannelName);
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(service != null)
        {
            service.createNotificationChannel(chan);
        }
        else
        {
            Log.w(TAG, "Could not get NotificationManager");
        }

        return channelId;
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
