package com.example.mpproject.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.mpproject.domain.model.PomodoroSettings;

public class PomodoroPreferences {

    private static final String Prefs_Name = "PomodoroPrefs";
    private final SharedPreferences prefs;

    public PomodoroPreferences(Context context){
        prefs = context.getSharedPreferences(Prefs_Name,Context.MODE_PRIVATE);
    }

    public PomodoroSettings load(){
        return new PomodoroSettings(
            prefs.getInt("focus", 25),
            prefs.getInt("short", 5),
            prefs.getInt("long", 15),
            prefs.getInt("sessions", 4),
            prefs.getString("theme", "Cat" )
        );
    }

    public void save(PomodoroSettings setting){
        prefs.edit()
                .putInt("focus",    setting.focusMin)
                .putInt("short",    setting.shortBreakMin)
                .putInt("long",     setting.longBreakMin)
                .putInt("sessions", setting.sessionsBeforeLB)
                .putString("theme", setting.theme)
                .apply();
    }
}
