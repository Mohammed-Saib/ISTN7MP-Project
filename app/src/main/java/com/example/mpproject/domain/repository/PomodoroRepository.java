package com.example.mpproject.domain.repository;

import com.example.mpproject.domain.model.PomodoroSettings;

public interface PomodoroRepository {
    PomodoroSettings loadSettings();
    void saveSettings(PomodoroSettings setting);
}
