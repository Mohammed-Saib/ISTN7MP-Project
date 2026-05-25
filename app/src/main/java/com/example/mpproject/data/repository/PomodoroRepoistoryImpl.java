package com.example.mpproject.data.repository;

import com.example.mpproject.data.local.PomodoroPreferences;
import com.example.mpproject.domain.model.PomodoroSetting;
import com.example.mpproject.domain.repository.PomodoroRepo;

public class PomodoroRepoistoryImpl implements PomodoroRepo {
    private final PomodoroPreferences prefs;

    public PomodoroRepoistoryImpl(PomodoroPreferences preferences){
        this.prefs = preferences;
    }

    @Override
    public PomodoroSetting loadSettings(){
        return prefs.load();
    }

    @Override
    public void saveSettings(PomodoroSetting setting){
        prefs.save(setting);
    }
}
