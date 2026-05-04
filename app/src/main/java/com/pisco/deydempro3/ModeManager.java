package com.pisco.deydempro3;

import android.content.Context;
import android.content.SharedPreferences;

public class ModeManager {

    private static final String PREF_NAME = "user_mode";
    private static final String KEY_MODE = "mode";

    public static final String MODE_CLIENT = "client";
    public static final String MODE_DRIVER = "driver";

    // 🔥 Sauvegarder mode
    public static void setMode(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_MODE, mode).apply();
    }

    // 🔥 Récupérer mode
    public static String getMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_MODE, MODE_CLIENT);
    }

    // 🔥 Vérifier chauffeur
    public static boolean isDriverMode(Context context) {
        return getMode(context).equals(MODE_DRIVER);
    }

    // 🔥 Vérifier client
    public static boolean isClientMode(Context context) {
        return getMode(context).equals(MODE_CLIENT);
    }
}