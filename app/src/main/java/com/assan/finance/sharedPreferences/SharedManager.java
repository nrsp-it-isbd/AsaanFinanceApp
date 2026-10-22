package com.assan.finance.sharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SharedManager {
    private static final String PREFERENCES_NAME = "MyAppPreferences_Enc";

    private static SharedManager instance;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private SharedManager(Context context) {
        SharedPreferences sp = null;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sp = EncryptedSharedPreferences.create(
                    context,
                    PREFERENCES_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback for extremely old/problematic devices
            sp = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
        sharedPreferences = sp;
        editor = sharedPreferences.edit();
    }

    public static SharedManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedManager(context);
        }
        return instance;
    }

    public void saveString(String key, String value) {
        editor.putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void saveInt(String key, int value) {
        editor.putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public void saveBoolean(String key, boolean value) {
        editor.putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void clearPreferences() {
        boolean isFirstTime = getBoolean(com.assan.finance.utils.Constants.isFirstTimeLaunch, true);
        boolean isDarkMode = getBoolean("DARK_MODE", false);
        boolean isKeepScreenOn = getBoolean("KEEP_SCREEN_ON", true);
        boolean isNotificationPopups = getBoolean("NOTIFICATION_POPUPS", true);
        
        editor.clear().apply();
        
        saveBoolean(com.assan.finance.utils.Constants.isFirstTimeLaunch, isFirstTime);
        saveBoolean("DARK_MODE", isDarkMode);
        saveBoolean("KEEP_SCREEN_ON", isKeepScreenOn);
        saveBoolean("NOTIFICATION_POPUPS", isNotificationPopups);
    }
}
