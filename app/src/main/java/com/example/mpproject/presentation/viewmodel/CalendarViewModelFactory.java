package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.CalendarEventRepository;
import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.TodoRepository;

// [ViewModel] Factory for CalendarViewModel — separate from ViewModelFactory to avoid polluting
// the existing auth/task factory with calendar-specific dependencies.
public class CalendarViewModelFactory implements ViewModelProvider.Factory {

    private final CalendarEventRepository eventRepo;
    private final TodoRepository todoRepo;
    private final ModuleRepository moduleRepo;
    private final String userId;

    public CalendarViewModelFactory(CalendarEventRepository eventRepo,
                                    TodoRepository todoRepo,
                                    ModuleRepository moduleRepo,
                                    String userId) {
        this.eventRepo  = eventRepo;
        this.todoRepo   = todoRepo;
        this.moduleRepo = moduleRepo;
        this.userId     = userId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CalendarViewModel.class)) {
            return (T) new CalendarViewModel(eventRepo, todoRepo, moduleRepo, userId);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
