package com.example.mpproject.domain.repository;

import com.example.mpproject.domain.model.PomodoroSetting;

public interface PomodoroRepo {
    PomodoroSetting loadSettings();
    void saveSettings(PomodoroSetting setting);
}
