package com.example.mpproject.data.repository;

import com.example.mpproject.data.local.PomodoroPreferences;
import com.example.mpproject.domain.model.PomodoroSettings;
import com.example.mpproject.domain.repository.PomodoroRepository;

public class PomodoroRepositoryImpl implements PomodoroRepository {
    private final PomodoroPreferences prefs;

    public PomodoroRepositoryImpl(PomodoroPreferences preferences){
        this.prefs = preferences;
    }

    @Override
    public PomodoroSettings loadSettings(){
        return prefs.load();
    }

    @Override
    public void saveSettings(PomodoroSettings setting){
        prefs.save(setting);
    }
}
