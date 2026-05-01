package com.pisco.deydempro3;

import android.content.Context;
import android.content.SharedPreferences;

public class ModeManager {

    private static final String PREF_NAME = "user_mode";
    private static final String KEY_MODE = "mode";

    public static final String MODE_CLIENT = "client";
    public static final String MODE_DRIVER = "driver";

    private SharedPreferences prefs;

    public ModeManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Sauvegarder mode
    public void setMode(String mode) {
        prefs.edit().putString(KEY_MODE, mode).apply();
    }

    // Récupérer mode
    public String getMode() {
        return prefs.getString(KEY_MODE, MODE_CLIENT);
    }

    // Vérifier mode chauffeur
    public boolean isDriverMode() {
        return getMode().equals(MODE_DRIVER);
    }

    // Vérifier mode client
    public boolean isClientMode() {
        return getMode().equals(MODE_CLIENT);
    }
}