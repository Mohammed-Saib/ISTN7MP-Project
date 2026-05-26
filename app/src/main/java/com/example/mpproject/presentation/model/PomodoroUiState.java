package com.example.mpproject.presentation.model;

public class PomodoroUiState {
    public String TimeDisplay;
    public int currentMode;
    public boolean isRunning;
    public String theme;
    public String activeTaskLabel;

    public PomodoroUiState(String timeDisplay, int currentMode, boolean isRunning, String theme,String activeTaskLabel){
        this.TimeDisplay = timeDisplay;
        this.currentMode = currentMode;
        this.isRunning = isRunning;
        this.theme = theme;
        this.activeTaskLabel = activeTaskLabel;
    }
}
