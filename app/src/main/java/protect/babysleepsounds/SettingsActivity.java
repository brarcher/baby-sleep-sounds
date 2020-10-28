package protect.babysleepsounds;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_wrapper, new Fragment()).commit();
    }

    public static class Fragment extends PreferenceFragmentCompat {

        private SeekBarPreference filterCutoff;
        private ListPreference theme;

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

            theme = findPreference("theme");
            theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Preferences.get(getActivity()).applyTheme((String) newValue);
                    return true;
                }
            });

            toggleCutoff(filterEnabled.isChecked());
            updateCutoffSummary(filterCutoff.getValue());
            updateThemeSummary();
        }

        private void updateThemeSummary() {
            theme.setSummary(theme.getEntry());
        }

        private void updateCutoffSummary(int value) {
            filterCutoff.setSummary(String.format(getString(R.string.filterCutoffValue), value));
        }

        private void toggleCutoff(boolean show) {
            filterCutoff.setVisible(show);
        }

    }
}
