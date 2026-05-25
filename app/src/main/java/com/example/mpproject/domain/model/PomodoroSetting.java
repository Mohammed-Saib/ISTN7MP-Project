package com.example.mpproject.domain.model;

public class PomodoroSetting {
    public int focusMin;
    public int shortBreakMin;
    public int longBreakMin;
    public int sessionsBeforeLB;
    public String theme;

    //constructor
    public PomodoroSetting(int focus,int SB,int LB, int Seshs, String Theme){
        this.focusMin = focus;
        this.shortBreakMin = SB;
        this.longBreakMin = LB;
        this.sessionsBeforeLB = Seshs;
        this.theme = Theme;
    }
}
