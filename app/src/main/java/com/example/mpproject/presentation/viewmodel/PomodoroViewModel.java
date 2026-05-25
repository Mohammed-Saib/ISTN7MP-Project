package com.example.mpproject.presentation.viewmodel;

import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.mpproject.domain.model.PomodoroSettings;
import com.example.mpproject.domain.repository.PomodoroRepository;
import com.example.mpproject.presentation.model.PomodoroUiState;

public class PomodoroViewModel extends ViewModel {

    public static final int MODE_FOCUS       = 0;
    public static final int MODE_SHORT_BREAK = 1;
    public static final int MODE_LONG_BREAK  = 2;

    private final PomodoroRepository repository;
    private PomodoroSettings settings;

    private CountDownTimer countDownTimer;
    private boolean isRunning = false;
    private long timeLeftMillis;
    private int currentMode = MODE_FOCUS;
    private int completedSessions = 0;

    private final MutableLiveData<PomodoroUiState> uiState = new MutableLiveData<>();
    public LiveData<PomodoroUiState> getUiState() { return uiState; }

    public PomodoroViewModel(PomodoroRepository repository) {
        this.repository = repository;
        settings = repository.loadSettings();
        setMode(MODE_FOCUS);
    }

    public void setMode(int mode) {
        cancelTimer();
        currentMode = mode;
        switch (mode) {
            case MODE_FOCUS:       timeLeftMillis = settings.focusMin * 60 * 1000L; break;
            case MODE_SHORT_BREAK: timeLeftMillis = settings.shortBreakMin * 60 * 1000L; break;
            case MODE_LONG_BREAK:  timeLeftMillis = settings.longBreakMin * 60 * 1000L; break;
        }
        emitState();
    }

    public void onPlayClicked() {
        if (isRunning) return;
        countDownTimer = new CountDownTimer(timeLeftMillis, 1000) {
            public void onTick(long ms) {
                timeLeftMillis = ms;
                emitState();
            }
            public void onFinish() {
                isRunning = false;
                onTimerFinished();
            }
        }.start();
        isRunning = true;
        emitState();
    }

    public void onPauseClicked() {
        cancelTimer();
        emitState();
    }

    public void saveSettings(PomodoroSettings newSettings) {
        settings = newSettings;
        repository.saveSettings(settings);
        setMode(currentMode);
    }

    public PomodoroSettings getSettings() { return settings; }

    private void onTimerFinished() {
        if (currentMode == MODE_FOCUS) {
            completedSessions++;
            if (completedSessions >= settings.sessionsBeforeLB) {
                completedSessions = 0;
                setMode(MODE_LONG_BREAK);
            } else {
                setMode(MODE_SHORT_BREAK);
            }
        } else {
            setMode(MODE_FOCUS);
        }
        onPlayClicked();
    }

    private void cancelTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;
    }

    private void emitState() {
        int minutes = (int)(timeLeftMillis / 1000) / 60;
        int seconds = (int)(timeLeftMillis / 1000) % 60;
        String display = String.format("%02d:%02d", minutes, seconds);
        uiState.setValue(new PomodoroUiState(display, currentMode, isRunning, settings.theme));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTimer();
    }
}
