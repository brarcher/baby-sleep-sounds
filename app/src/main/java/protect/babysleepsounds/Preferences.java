package protect.babysleepsounds;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    private static final String LOW_PASS_FILTER_ENABLED = "lowPassFilterEnabled";
    private static final String LOW_PASS_FILTER_FREQUENCY = "lowPassFilterFrequency";

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

    public void setLowPassFilterEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(LOW_PASS_FILTER_ENABLED, enabled)
                .apply();
    }

    public boolean isLowPassFilterEnabled() {
        return preferences.getBoolean(LOW_PASS_FILTER_ENABLED, false);
    }

    public void setLowPassFilterFrequency(int frequency) {
        preferences.edit()
                .putInt(LOW_PASS_FILTER_FREQUENCY, frequency)
                .apply();
    }

    public int getLowPassFilterFrequency() {
        return preferences.getInt(LOW_PASS_FILTER_FREQUENCY, 1000);
    }

}
