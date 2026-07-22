package com.example.mpproject.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalSessionManager {

    private static final String PREF_NAME = "app_prefs";
    private static final String KEY_USER_ID = "current_user_id";

    private LocalSessionManager() {}

    public static void setCurrentUserId(Context context, String userId) {
        getPrefs(context).edit().putString(KEY_USER_ID, userId).apply();
    }

    public static String getCurrentUserId(Context context) {
        return getPrefs(context).getString(KEY_USER_ID, null);
    }

    public static void clearSession(Context context) {
        getPrefs(context).edit().remove(KEY_USER_ID).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
