package protect.babysleepsounds;

import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
public class MainActivity extends AppCompatActivity
{
    private final static String TAG = "BabySleepSounds";

    private Map<String, Integer> _soundMap;
    private Map<String, Integer> _timeMap;

    private MediaPlayer _mediaPlayer = null;
    private Timer _timer = null;
    private FFmpeg _ffmpeg;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        _soundMap = ImmutableMap.<String, Integer>builder()
            .put(getResources().getString(R.string.dryer), R.raw.dryer)
            .put(getResources().getString(R.string.ocean), R.raw.ocean)
            .put(getResources().getString(R.string.rain), R.raw.rain)
            .put(getResources().getString(R.string.refrigerator), R.raw.refrigerator)
            .put(getResources().getString(R.string.train), R.raw.train)
            .put(getResources().getString(R.string.vacuum), R.raw.vacuum)
            .put(getResources().getString(R.string.water), R.raw.water)
            .put(getResources().getString(R.string.waterfall), R.raw.waterfall)
            .put(getResources().getString(R.string.waves), R.raw.waves)
            .put(getResources().getString(R.string.whiteNoise), R.raw.white_noise)
            .build();

        _timeMap = ImmutableMap.<String, Integer>builder()
                .put(getResources().getString(R.string.disabled), 0)
                .put(getResources().getString(R.string.time_1minute), 1000*60*1)
                .put(getResources().getString(R.string.time_5minute), 1000*60*5)
                .put(getResources().getString(R.string.time_10minute), 1000*60*10)
                .put(getResources().getString(R.string.time_30minute), 1000*60*30)
                .put(getResources().getString(R.string.time_1hour), 1000*60*60*1)
                .put(getResources().getString(R.string.time_2hour), 1000*60*60*2)
                .put(getResources().getString(R.string.time_4hour), 1000*60*60*4)
                .put(getResources().getString(R.string.time_8hour), 1000*60*60*8)
                .build();

        final Spinner soundSpinner = (Spinner) findViewById(R.id.soundSpinner);

        List<String> names = new ArrayList<>(_soundMap.keySet());

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        soundSpinner.setAdapter(dataAdapter);


        final Spinner sleepTimeoutSpinner = (Spinner) findViewById(R.id.sleepTimerSpinner);
        List<String> times = new ArrayList<>(_timeMap.keySet());

        ArrayAdapter<String> timesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, times);
        timesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sleepTimeoutSpinner.setAdapter(timesAdapter);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(_mediaPlayer == null)
                {
                    startPlayback();
                }
                else
                {
                    stopPlayback();
                }
            }
        });

        _ffmpeg = FFmpeg.getInstance(this);

        try
        {
            _ffmpeg.loadBinary(new LoadBinaryResponseHandler()
            {

                @Override
                public void onStart()
                {
                    Log.d(TAG, "ffmpeg.loadBinary onStart()");
                }

                @Override
                public void onFailure()
                {
                    Log.d(TAG, "ffmpeg.loadBinary onFailure()");
                    reportPlaybackUnsupported();
                }

                @Override
                public void onSuccess()
                {
                    Log.d(TAG, "ffmpeg.loadBinary onSuccess()");
                    button.setEnabled(true);
                }

                @Override
                public void onFinish()
                {
                    Log.d(TAG, "ffmpeg.loadBinary onFinish()");
                }
            });
        }
        catch (FFmpegNotSupportedException e)
        {
            Log.d(TAG, "ffmpeg not supported", e);
            reportPlaybackUnsupported();
        }
    }

    /**
     * Report to the user that playback is not supported on this device
     */
    private void reportPlaybackUnsupported()
    {
        Toast.makeText(this, R.string.playbackNotSupported, Toast.LENGTH_LONG).show();
    }

    private void startPlayback()
    {
        final Spinner soundSpinner = (Spinner) findViewById(R.id.soundSpinner);
        String selectedSound = (String)soundSpinner.getSelectedItem();

        int id = _soundMap.get(selectedSound);

        _mediaPlayer = MediaPlayer.create(MainActivity.this, id);
        _mediaPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        _mediaPlayer.setLooping(true);
        _mediaPlayer.start();

        final Spinner sleepTimeoutSpinner = (Spinner) findViewById(R.id.sleepTimerSpinner);
        String selectedTimeout = (String)sleepTimeoutSpinner.getSelectedItem();
        int timeoutMs = _timeMap.get(selectedTimeout);
        if(timeoutMs > 0)
        {
            _timer = new Timer();
            _timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    stopPlayback();
                }
            }, (long)timeoutMs);
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                final Button button = (Button) findViewById(R.id.button);
                button.setText(R.string.stop);
            }
        });
    }

    private void stopPlayback()
    {
        _mediaPlayer.stop();
        _mediaPlayer.release();
        _mediaPlayer = null;

        if(_timer != null)
        {
            _timer.cancel();
            _timer.purge();
            _timer = null;
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                final Button button = (Button) findViewById(R.id.button);
                button.setText(R.string.play);
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        if(_mediaPlayer != null)
        {
            _mediaPlayer.stop();
            _mediaPlayer.release();
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == R.id.action_about)
        {
            displayAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayAboutDialog()
    {
        final String[][] USED_LIBRARIES = new String[][]
        {
                new String[] {"FFmpeg", "https://ffmpeg.org/"},
                new String[] {"FFmpeg-Android", "https://github.com/writingminds/ffmpeg-android"},
        };

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (String[] library : USED_LIBRARIES)
        {
            libs.append("<li><a href=\"").append(library[1]).append("\">").append(library[0]).append("</a></li>");
        }
        libs.append("</ul>");

        String appName = getString(R.string.app_name);
        int year = Calendar.getInstance().get(Calendar.YEAR);

        String version = "?";
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.w(TAG, "Package name not found", e);
        }

        final String[][] SOUND_RESOURCES = new String[][]
        {
            new String[] {"Canton Becker", "http://whitenoise.cantonbecker.com/"},
            new String[] {"The MC2 Method", "http://mc2method.org/white-noise/"},
        };

        StringBuilder soundResources = new StringBuilder().append("<ul>");
        for (String[] resource : SOUND_RESOURCES)
        {
            soundResources.append("<li><a href=\"").append(resource[1]).append("\">").append(resource[0]).append("</a></li>");
        }
        soundResources.append("</ul>");

        WebView wv = new WebView(this);
        String html =
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" +
            "<img src=\"file:///android_res/mipmap/ic_launcher.png\" alt=\"" + appName + "\"/>" +
            "<h1>" +
            String.format(getString(R.string.about_title_fmt),
                    "<a href=\"" + getString(R.string.app_webpage_url)) + "\">" +
            appName +
            "</a>" +
            "</h1><p>" +
            appName +
            " " +
            String.format(getString(R.string.debug_version_fmt), version) +
            "</p><p>" +
            String.format(getString(R.string.app_revision_fmt),
                    "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                            getString(R.string.app_revision_url) +
                            "</a>") +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_copyright_fmt), year) +
            "</p><hr/><p>" +
            getString(R.string.app_license) +
            "</p><hr/><p>" +
            String.format(getString(R.string.sound_resources), appName, soundResources.toString()) +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_libraries), appName, libs.toString());

        wv.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
        new AlertDialog.Builder(this)
            .setView(wv)
            .setCancelable(true)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            })
            .show();
    }
}
