package com.example.mpproject.presentation.viewmodel;

import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.data.local.entity.TodoEntity;
import com.example.mpproject.domain.model.PomodoroSettings;
import com.example.mpproject.domain.repository.PomodoroRepository;
import com.example.mpproject.presentation.model.PomodoroUiState;

import java.util.ArrayList;
import java.util.List;

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

    private String activeTaskLabel = null; // label shown above focus tabs
    private final MutableLiveData<List<TodoEntity>> recommendedTasks = new MutableLiveData<>();
    public LiveData<List<TodoEntity>> getRecommendedTasks() { return recommendedTasks; }
    private final MutableLiveData<List<String>> myTasks = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<String>> getMyTasks() { return myTasks;}

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
        uiState.setValue(new PomodoroUiState(display, currentMode, isRunning, settings.theme, activeTaskLabel));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTimer();
    }

    // Call this from TimerFragment after querying Room
    public void setRecommendedTasks(List<TodoEntity> tasks) {
        // Sort: HIGH priority first, then by due date ascending
        List<TodoEntity> sorted = new ArrayList<>(tasks);
        sorted.sort((a, b) -> {
            int pa = priorityScore(a.getPriority());
            int pb = priorityScore(b.getPriority());
            if (pa != pb) return pb - pa; // higher score = higher priority first
            Long da = a.getDueDate(), db = b.getDueDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return Long.compare(da, db);
        });
        recommendedTasks.setValue(sorted);
    }

    private int priorityScore(String p) {
        if ("HIGH".equals(p))   return 3;
        if ("MEDIUM".equals(p)) return 2;
        if ("LOW".equals(p))    return 1;
        return 0;
    }

    public void setActiveTask(String label) {
        activeTaskLabel = label;
        emitState();
    }

    public void clearActiveTask() {
        activeTaskLabel = null;
        emitState();
    }
    public void addMyTask(String label) {
        List<String> current = new ArrayList<>(myTasks.getValue() != null
                ? myTasks.getValue() : new ArrayList<>());
        current.add(label);
        myTasks.setValue(current);
    }

    public void removeMyTask(int index) {
        List<String> current = new ArrayList<>(myTasks.getValue() != null
                ? myTasks.getValue() : new ArrayList<>());
        if (index >= 0 && index < current.size()) {
            current.remove(index);
            myTasks.setValue(current);
        }
    }

    // Accept all recommended into My Tasks
    public void acceptAllRecommended() {
        List<TodoEntity> recommended = recommendedTasks.getValue();
        if (recommended == null) return;
        List<String> current = new ArrayList<>(myTasks.getValue() != null
                ? myTasks.getValue() : new ArrayList<>());
        for (TodoEntity t : recommended) {
            if (!current.contains(t.getTitle())) {
                current.add(t.getTitle());
            }
        }
        myTasks.setValue(current);
    }
}
