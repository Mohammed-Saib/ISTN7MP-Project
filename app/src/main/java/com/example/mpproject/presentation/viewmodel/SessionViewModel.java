package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.model.Todo;

import java.util.ArrayList;
import java.util.List;

// Shared ViewModel — scoped to the Activity so TasksFragment and TimerFragment
// both see the same session task list.
public class SessionViewModel extends ViewModel {

    private final MutableLiveData<List<String>> sessionTasks =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<String>> getSessionTasks() { return sessionTasks; }

    public void setSessionTasks(List<String> tasks) {
        sessionTasks.setValue(new ArrayList<>(tasks));
    }

    public void addTask(String label) {
        List<String> current = currentList();
        if (!label.trim().isEmpty() && !current.contains(label)) {
            current.add(label);
            sessionTasks.setValue(current);
        }
    }

    public void removeTask(int index) {
        List<String> current = currentList();
        if (index >= 0 && index < current.size()) {
            current.remove(index);
            sessionTasks.setValue(current);
        }
    }

    public void clearAll() {
        sessionTasks.setValue(new ArrayList<>());
    }

    private List<String> currentList() {
        return new ArrayList<>(
                sessionTasks.getValue() != null ? sessionTasks.getValue() : new ArrayList<>());
    }
}