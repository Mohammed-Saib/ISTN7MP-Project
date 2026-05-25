package com.example.mpproject.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.mpproject.domain.model.PomodoroSetting;

public class PomodoroPreferences {

    private static final String Prefs_Name = "PomodoroPrefs";
    private final SharedPreferences prefs;

    public PomodoroPreferences(Context context){
        prefs = context.getSharedPreferences(Prefs_Name,Context.MODE_PRIVATE);
    }

    public PomodoroSetting load(){
        return new PomodoroSetting(
            prefs.getInt("focus", 25),
            prefs.getInt("SB", 5),
            prefs.getInt("LB", 15),
            prefs.getInt("Seshs", 4),
            prefs.getString("theme", "Default" )
        );
    }

    public void save(PomodoroSetting setting){
        prefs.edit()
                .putInt("focus",    setting.focusMin)
                .putInt("short",    setting.shortBreakMin)
                .putInt("long",     setting.longBreakMin)
                .putInt("sessions", setting.sessionsBeforeLB)
                .putString("theme", setting.theme)
                .apply();
    }
}
