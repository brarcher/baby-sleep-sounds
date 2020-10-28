package protect.babysleepsounds;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class LoopingAudioPlayer
{
    public static final String TAG = "BabySleepSounds";

    private static final int FREQUENCY = 44100;
    private static final int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final File _wavFile;
    private final PowerManager.WakeLock _wakeLock;

    public LoopingAudioPlayer(Context context, File wavFile)
    {
        _wavFile = wavFile;

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if(powerManager != null)
        {
            _wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "protect.babysleepsounds:LoopingAudioPlayerWakeLock");
        }
        else
        {
            Log.w(TAG, "Failed to acquire a wakelock");
            _wakeLock = null;
        }
    }

    public void start()
    {
        if(_playbackThread != null)
        {
            if(_wakeLock != null)
            {
                _wakeLock.acquire();
            }

            _playbackThread.start();
        }
        else
        {
            Log.w(TAG, "Audio playback already stopped, cannot start again");
        }
    }

    public void stop()
    {
        Log.i(TAG, "Requesting audio playback to stop");

        if(_playbackThread != null)
        {
            _playbackThread.interrupt();
            _playbackThread = null;

            if(_wakeLock != null)
            {
                _wakeLock.release();
            }
        }
    }

    private Thread _playbackThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            Log.i(TAG, "Setting up audio playback");

            final int bufferSize = AudioTrack.getMinBufferSize(FREQUENCY, CHANNEL_CONFIGURATION, AUDIO_ENCODING);
            final int byteBufferSize = bufferSize*2;

            final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    FREQUENCY,
                    CHANNEL_CONFIGURATION,
                    AUDIO_ENCODING,
                    bufferSize,
                    AudioTrack.MODE_STREAM);

            FileInputStream is = null;

            try
            {
                final byte [] buffer = new byte[byteBufferSize];
                audioTrack.play();

                while(Thread.currentThread().isInterrupted() == false)
                {
                    is = new FileInputStream(_wavFile);
                    int read;

                    while(Thread.currentThread().isInterrupted() == false)
                    {
                        read = is.read(buffer);

                        if(read <= 0)
                        {
                            break;
                        }

                        audioTrack.write(buffer, 0, read);
                    }

                    // File completed playback, start again
                    try
                    {
                        is.close();
                    }
                    catch(IOException e)
                    {
                        // Nothing to do, we are finished with the file anyway
                    }
                }
            }
            catch(IOException e)
            {
                Log.d(TAG, "Failed to read file", e);
            }
            finally
            {
                try
                {
                    if(is != null)
                    {
                        is.close();
                    }
                }
                catch(IOException e)
                {
                    Log.d(TAG, "Failed to close file", e);
                }

                audioTrack.release();
            }

            Log.i(TAG, "Finished playback");
        }
    }, "PlaybackThread");
}