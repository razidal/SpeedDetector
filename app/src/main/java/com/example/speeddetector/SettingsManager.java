package com.example.speeddetector;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static SettingsManager instance;
    private SharedPreferences preferences;

    private SettingsManager(Context context) {
        preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    public boolean isDarkMode() {
        return preferences.getBoolean("isDarkMode", false);
    }

    public void setDarkMode(boolean isDarkMode) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isDarkMode", isDarkMode);
        editor.apply();
    }

    public String getUnitPreference() {
        return preferences.getString("unit", "m/s");
    }

    public void setUnitPreference(String unit) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("unit", unit);
        editor.apply();
    }
}
