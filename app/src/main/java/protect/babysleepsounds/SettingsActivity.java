package protect.babysleepsounds;

import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.common.collect.ImmutableMap;

import java.util.Calendar;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_wrapper, new Fragment()).commit();
    }

    public static class Fragment extends PreferenceFragmentCompat {

        private SeekBarPreference filterCutoff;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);

            filterCutoff = findPreference("filter_cutoff");

            filterCutoff.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    updateCutoffSummary((Integer) newValue);
                    return true;
                }
            });

            SwitchPreferenceCompat filterEnabled = findPreference("filter_enabled");

            filterEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    toggleCutoff((Boolean) newValue);
                    return true;
                }
            });

            toggleCutoff(filterEnabled.isChecked());
            updateCutoffSummary(filterCutoff.getValue());
        }

        private void updateCutoffSummary(int value) {
            filterCutoff.setSummary(String.format(getString(R.string.filterCutoffValue), value));
        }

        private void toggleCutoff(boolean show) {
            filterCutoff.setVisible(show);
        }

    }
}
