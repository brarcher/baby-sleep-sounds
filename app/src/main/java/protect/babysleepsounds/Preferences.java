package protect.babysleepsounds;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class Preferences {

    private static final String LOW_PASS_FILTER_ENABLED = "filter_enabled";
    private static final String LOW_PASS_FILTER_FREQUENCY = "filter_value";
    private static final String THEME = "theme";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static Preferences instance;

    private final SharedPreferences preferences;

    private Preferences(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Preferences get(Context context) {
        if (instance == null) {
            instance = new Preferences(context);
        }

        return instance;
    }

    public boolean isLowPassFilterEnabled() {
        return preferences.getBoolean(LOW_PASS_FILTER_ENABLED, false);
    }

    public int getLowPassFilterFrequency() {
        return preferences.getInt(LOW_PASS_FILTER_FREQUENCY, 1000);
    }

    public String getTheme() {
        return preferences.getString(THEME, THEME_LIGHT);
    }

    public void applyTheme() {
        applyTheme(getTheme());
    }

    public void applyTheme(String theme) {
        int dayNightMode;
        switch (theme) {
            case THEME_LIGHT:
                dayNightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;

            case THEME_DARK:
                dayNightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;

            default:
                dayNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        AppCompatDelegate.setDefaultNightMode(dayNightMode);
    }
}
