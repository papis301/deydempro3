package deydemv3;

import android.content.Context;
import android.content.SharedPreferences;

public class ModeManager {

    private static final String PREF_NAME = "user_mode_pref";
    private static final String KEY_MODE = "current_mode";

    public static final String MODE_CLIENT = "client";
    public static final String MODE_DRIVER = "driver";

    private SharedPreferences prefs;

    public ModeManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getMode() {
        return prefs.getString(KEY_MODE, MODE_CLIENT);
    }

    public void setMode(String mode) {
        prefs.edit().putString(KEY_MODE, mode).apply();
    }

    public String toggleMode() {
        String current = getMode();
        String newMode = current.equals(MODE_CLIENT) ? MODE_DRIVER : MODE_CLIENT;
        setMode(newMode);
        return newMode;
    }

    public boolean isDriverMode() {
        return getMode().equals(MODE_DRIVER);
    }

    public boolean isClientMode() {
        return getMode().equals(MODE_CLIENT);
    }
}
