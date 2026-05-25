package com.example.mpproject.domain.model;

public class PomodoroSettings {
    public int focusMin;
    public int shortBreakMin;
    public int longBreakMin;
    public int sessionsBeforeLB;
    public String theme;

    //constructor
    public PomodoroSettings(int focus, int SB, int LB, int sessions, String Theme){
        this.focusMin = focus;
        this.shortBreakMin = SB;
        this.longBreakMin = LB;
        this.sessionsBeforeLB = sessions;
        this.theme = Theme;
    }
}
