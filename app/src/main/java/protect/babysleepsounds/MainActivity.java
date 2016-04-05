package protect.babysleepsounds;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
{
    private Map<String, Integer> _soundMap;

    MediaPlayer _mediaPlayer = null;

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

        final Spinner soundSpinner = (Spinner) findViewById(R.id.soundSpinner);

        List<String> names = new ArrayList<>(_soundMap.keySet());

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        soundSpinner.setAdapter(dataAdapter);

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
}
