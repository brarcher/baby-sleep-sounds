package protect.babysleepsounds;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class LoopingAudioPlayer
{
    public static final String TAG = "BabySleepSounds";

    private final File _wavFile;
    private Thread _thread1;
    private Thread _thread2;

    public LoopingAudioPlayer(File wavFile)
    {
        _wavFile = wavFile;
    }

    public void start()
    {
        stop();

        ToggleWaiter first = new ToggleWaiter();
        ToggleWaiter second = new ToggleWaiter();

        _thread1 = new Thread(new FadingAudioPlayer(_wavFile, first, second), "PlayThread1");
        _thread2 = new Thread(new FadingAudioPlayer(_wavFile, second, first), "PlayThread2");

        _thread1.start();
        _thread2.start();

        // Start the first thread playing
        first.set();
    }

    public void stop()
    {
        Log.i(TAG, "Requesting audio playback to stop");

        if(_thread1 != null && _thread2 != null)
        {
            _thread1.interrupt();
            _thread2.interrupt();

            _thread1 = null;
            _thread2 = null;
        }
    }
}

class FadingAudioPlayer implements Runnable
{
    public static final String TAG = "BabySleepSounds";

    private static final int OVERLAP_AUDIO_START_SEC = 3;

    private static final int FREQUENCY = 44100;
    private static final int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final File _wavFile;
    private final ToggleWaiter _waitToggle;
    private final ToggleWaiter _releaseToggle;

    public FadingAudioPlayer(File wavFile, ToggleWaiter waitToggle, ToggleWaiter releaseToggle)
    {
        _wavFile = wavFile;
        _waitToggle = waitToggle;
        _releaseToggle = releaseToggle;
    }

    public void run()
    {
        Log.i(TAG, "Setting up stream");

        final int bufferSize = AudioTrack.getMinBufferSize(FREQUENCY, CHANNEL_CONFIGURATION, AUDIO_ENCODING);
        final int byteBufferSize = bufferSize*2;

        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                FREQUENCY,
                CHANNEL_CONFIGURATION,
                AUDIO_ENCODING,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener()
        {
            @Override
            public void onMarkerReached(AudioTrack track)
            {
                Log.d(TAG, "Starting other audio thread to overlap");
                _releaseToggle.set();
            }

            @Override
            public void onPeriodicNotification(AudioTrack track)
            {
                // Nothing to do
            }
        });

        int framesInFile = (int)_wavFile.length()/2/2;
        int fadeOutFrames = OVERLAP_AUDIO_START_SEC * FREQUENCY;
        int fadeOutFrameStart = framesInFile - fadeOutFrames;

        FileInputStream is = null;

        try
        {
            while(Thread.currentThread().isInterrupted() == false)
            {
                // Wait to start
                _waitToggle.waitUntilSet();

                // The file contains bytes, but the PCM data in 16 bit, so /2 to get samples.
                // The file also is stereo instead of mono, so /2 for two different tracks.

                audioTrack.setNotificationMarkerPosition(fadeOutFrameStart);

                is = new FileInputStream(_wavFile);
                final byte [] buffer = new byte[byteBufferSize];
                int read;

                audioTrack.play();

                while(Thread.currentThread().isInterrupted() == false)
                {
                    read = is.read(buffer);

                    if(read <= 0)
                    {
                        break;
                    }

                    audioTrack.write(buffer, 0, read);
                }

                audioTrack.stop();
            }
        }
        catch(IOException e)
        {
            Log.d(TAG, "Failed to read file", e);
        }
        catch(InterruptedException e)
        {
            Log.d(TAG, "Terminating due to being interrupted", e);
        }
        finally
        {
            audioTrack.release();

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
        }

        Log.i(TAG, "Finished playback");
    }
}

class ToggleWaiter
{
    private boolean isSet = false;

    public void set()
    {
        synchronized(this)
        {
            isSet = true;
            notify();
        }
    }

    public void waitUntilSet() throws InterruptedException
    {
        synchronized(this)
        {
            while(isSet == false)
            {
                wait();
            }

            isSet = false;
        }
    }


}