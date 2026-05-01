package deydemv3;


import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {

    private static final String PREF_NAME = "user_data";
    private static final String KEY_IS_DRIVER = "is_driver";

    private SharedPreferences prefs;

    public UserManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setDriver(boolean isDriver) {
        prefs.edit().putBoolean(KEY_IS_DRIVER, isDriver).apply();
    }

    public boolean isDriver() {
        return prefs.getBoolean(KEY_IS_DRIVER, false);
    }
}
