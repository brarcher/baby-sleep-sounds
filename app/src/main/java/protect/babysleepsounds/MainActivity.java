package protect.babysleepsounds;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = "BabySleepSounds";

    private static final String ORIGINAL_MP3_FILE = "original.mp3";
    private static final String PROCESSED_RAW_FILE = "processed.raw";

    private Map<String, Integer> _soundMap;
    private Map<String, Integer> _timeMap;

    private boolean _playing = false;
    private Timer _timer;

    private FFmpeg _ffmpeg;
    private CheckBox _enableFilterSetting;
    private SeekBar _filterCutoffFrequencySetting;
    private ProgressDialog _encodingProgress;

    private CheckBox _useDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        final SharedPreferences pref = getApplicationContext().getSharedPreferences(TAG, MODE_PRIVATE);
        if (pref.getBoolean("useDarkTheme",false)) {
            setTheme(R.style.AppThemeDark_NoActionBar );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // These sound files by convention are:
        // - take a ~10 second clip
        // - Apply a 2 second fade-in and fade-out
        // - Cut the first 3 seconds, and place it over the last three seconds
        //   which should create a seamless track appropriate for looping
        // - Save as a mp3 file, 128kbps, stereo
        _soundMap = ImmutableMap.<String, Integer>builder()
            .put(getResources().getString(R.string.campfire), R.raw.campfire)
            .put(getResources().getString(R.string.dryer), R.raw.dryer)
            .put(getResources().getString(R.string.fan), R.raw.fan)
            .put(getResources().getString(R.string.ocean), R.raw.ocean)
            .put(getResources().getString(R.string.rain), R.raw.rain)
            .put(getResources().getString(R.string.refrigerator), R.raw.refrigerator)
            .put(getResources().getString(R.string.shhhh), R.raw.shhhh)
            .put(getResources().getString(R.string.shower), R.raw.shower)
            .put(getResources().getString(R.string.stream), R.raw.stream)
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

        final Spinner soundSpinner = findViewById(R.id.soundSpinner);

        List<String> names = new ArrayList<>(_soundMap.keySet());

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        soundSpinner.setAdapter(dataAdapter);


        final Spinner sleepTimeoutSpinner = findViewById(R.id.sleepTimerSpinner);
        List<String> times = new ArrayList<>(_timeMap.keySet());
        sleepTimeoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if(_playing)
                {
                    updatePlayTimeout();
                    Toast.makeText(MainActivity.this, R.string.sleepTimerUpdated, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // noop
            }
        });

        ArrayAdapter<String> timesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, times);
        timesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sleepTimeoutSpinner.setAdapter(timesAdapter);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(_playing == false)
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

        final View filterFrequencyReadout = findViewById(R.id.filterFrequencyReadout);
        final View filterFrequencyLayout = findViewById(R.id.filterFrequencyLayout);
        _enableFilterSetting = findViewById(R.id.enableFilter);
        _enableFilterSetting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                filterFrequencyLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                filterFrequencyReadout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        _filterCutoffFrequencySetting = findViewById(R.id.filterFrequencyBar);
        _filterCutoffFrequencySetting.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                updateFrequencyReadout(getFrequencyReadout());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                // Nothing to do
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                // Nothing to do
            }
        });

        // Set initial value
        updateFrequencyReadout(getFrequencyReadout());

        _useDarkTheme = findViewById(R.id.useDarkTheme);
        _useDarkTheme.setChecked(pref.getBoolean("useDarkTheme",false));
        _useDarkTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("useDarkTheme", isChecked);
                editor.commit();
                recreate(); //Apply theme change immediately
            }
        });
    }

    /**
     * Retrieve the value of the frequency readout, accounting for any
     * offset or adjustments
     */
    private int getFrequencyReadout()
    {
        int rawValue = _filterCutoffFrequencySetting.getProgress();
        // The data represented by the frequency bar starts at 200, so we need to add
        // this offset to the value, as the bar's value always starts at 0.
        int actualValue = 200 + rawValue;
        return actualValue;
    }

    /**
     * Update the TextView containing the frequency to the given value
     * @param frequency value to update the field with
     */
    private void updateFrequencyReadout(int frequency)
    {
        final TextView filterFrequency = findViewById(R.id.filterFrequencyText);
        String readout = String.format(getString(R.string.filterCutoff), frequency);
        filterFrequency.setText(readout);
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
        final Spinner soundSpinner = findViewById(R.id.soundSpinner);
        String selectedSound = (String)soundSpinner.getSelectedItem();
        int id = _soundMap.get(selectedSound);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        try
        {
            File originalFile = new File(getFilesDir(), ORIGINAL_MP3_FILE);

            Log.i(TAG, "Writing file out prior to WAV conversion");
            writeToFile(id, originalFile);

            final File processed = new File(getFilesDir(), PROCESSED_RAW_FILE);
            if(processed.exists())
            {
                boolean result = processed.delete();
                if(result == false)
                {
                    throw new IOException("Unable to delete previous file, cannot prepare new file");
                }
            }

            Log.i(TAG, "Converting file to WAV");

            LinkedList<String> arguments = new LinkedList<>();
            arguments.add("-i");
            arguments.add(originalFile.getAbsolutePath());
            if(_enableFilterSetting.isChecked())
            {
                int frequencyValue = getFrequencyReadout();

                Log.i(TAG, "Will perform lowpass filter to " + frequencyValue + " Hz");
                arguments.add("-af");
                arguments.add("lowpass=frequency=" + frequencyValue);
            }
            arguments.add("-f");
            arguments.add("s16le");
            arguments.add("-acodec");
            arguments.add("pcm_s16le");
            arguments.add(processed.getAbsolutePath());

            _encodingProgress = new ProgressDialog(this);
            _encodingProgress.setMessage(getString(R.string.preparing));
            _encodingProgress.show();

            Log.i(TAG, "Launching ffmpeg");
            String[] cmd = arguments.toArray(new String[arguments.size()]);
            _ffmpeg.execute(cmd, new FFmpegExecuteResponseHandler()
            {
                public void onStart()
                {
                    Log.d(TAG, "ffmpeg execute onStart()");
                }

                public void onSuccess(String message)
                {
                    Log.d(TAG, "ffmpeg execute onSuccess(): " + message);

                    Intent startIntent = new Intent(MainActivity.this, AudioService.class);
                    startIntent.putExtra(AudioService.AUDIO_FILENAME_ARG, processed.getAbsolutePath());
                    startService(startIntent);

                    updateToPlaying();
                }

                public void onProgress(String message)
                {
                    Log.d(TAG, "ffmpeg execute onProgress(): " + message);
                }

                public void onFailure(String message)
                {
                    Log.d(TAG, "ffmpeg execute onFailure(): " + message);
                    reportPlaybackFailure();
                }

                public void onFinish()
                {
                    Log.d(TAG, "ffmpeg execute onFinish()");
                }
            });
        }
        catch(IOException|FFmpegCommandAlreadyRunningException e)
        {
            Log.i(TAG, "Failed to start playback", e);
            reportPlaybackFailure();
        }
    }

    /**
     * Write a resource to a file
     * @param resource resource to write
     * @param output destination of the resource
     * @throws IOException if a write failure occurs
     */
    private void writeToFile(int resource, File output) throws IOException
    {
        InputStream rawStream = getResources().openRawResource(resource);
        FileOutputStream outStream = null;

        byte[] buff = new byte[1024];
        int read;

        try
        {
            outStream = new FileOutputStream(output);

            while ((read = rawStream.read(buff)) > 0)
            {
                outStream.write(buff, 0, read);
            }
        }
        finally
        {
            try
            {
                rawStream.close();
            }
            catch(IOException e)
            {
                // If it fails, there is nothing to do
            }

            if(outStream != null)
            {
                outStream.close();
            }
        }
    }

    /**
     * Report to the user that playback has failed, and hide the progress dialog
     */
    private void reportPlaybackFailure()
    {
        if(_encodingProgress != null)
        {
            _encodingProgress.dismiss();
            _encodingProgress = null;
        }

        Toast.makeText(this, R.string.playbackFailure, Toast.LENGTH_LONG).show();
    }

    /**
     * Update the timeout for playback to stop
     */
    private void updatePlayTimeout()
    {
        // Cancel the running timer
        if(_timer != null)
        {
            _timer.cancel();
            _timer.purge();
        }

        final Spinner sleepTimeoutSpinner = findViewById(R.id.sleepTimerSpinner);
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
    }

    /**
     * Update the UI to reflect it is playing
     */
    private void updateToPlaying()
    {
        _playing = true;

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                updatePlayTimeout();

                final Button button = findViewById(R.id.button);
                button.setText(R.string.stop);

                setControlsEnabled(false);

                if(_encodingProgress != null)
                {
                    _encodingProgress.hide();
                    _encodingProgress = null;
                }
            }
        });
    }

    private void stopPlayback()
    {
        Intent stopIntent = new Intent(MainActivity.this, AudioService.class);
        startService(stopIntent);

        _playing = false;

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
                final Button button = findViewById(R.id.button);
                button.setText(R.string.play);

                setControlsEnabled(true);
            }
        });
    }

    private void setControlsEnabled(boolean enabled)
    {
        for(int resId : new int[]{R.id.soundSpinner, R.id.enableFilter, R.id.filterFrequencyBar, R.id.useDarkTheme})
        {
            final View view = findViewById(resId);
            view.setEnabled(enabled);
        }
    }

    @Override
    protected void onDestroy()
    {
        if(_playing)
        {
            stopPlayback();
        }

        for(String toDelete : new String[]{ORIGINAL_MP3_FILE, PROCESSED_RAW_FILE})
        {
            File file = new File(getFilesDir(), toDelete);
            boolean result = file.delete();
            if(result == false)
            {
                Log.w(TAG, "Failed to delete file on exit: " + file.getAbsolutePath());
            }
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
        final Map<String, String> USED_LIBRARIES = ImmutableMap.of
        (
            "FFmpeg", "https://ffmpeg.org/",
            "FFmpeg-Android", "https://github.com/writingminds/ffmpeg-android"
        );

        final Map<String, String> SOUND_RESOURCES = ImmutableMap.of
        (
            "Canton Becker", "http://whitenoise.cantonbecker.com/",
            "The MC2 Method", "http://mc2method.org/white-noise/",
            "Campfire-1.mp3 Copyright SoundJay.com Used with Permission", "https://www.soundjay.com/nature/campfire-1.mp3"
        );

        final Map<String, String> IMAGE_RESOURCES = ImmutableMap.of
        (
            "'Music' by Aleks from the Noun Project", "https://thenounproject.com/term/music/886761/"
        );

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : USED_LIBRARIES.entrySet())
        {
            libs.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        libs.append("</ul>");

        StringBuilder soundResources = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : SOUND_RESOURCES.entrySet())
        {
            soundResources.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        soundResources.append("</ul>");

        StringBuilder imageResources = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : IMAGE_RESOURCES.entrySet())
        {
            imageResources.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        imageResources.append("</ul>");

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
            String.format(getString(R.string.image_resources), appName, imageResources.toString()) +
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
