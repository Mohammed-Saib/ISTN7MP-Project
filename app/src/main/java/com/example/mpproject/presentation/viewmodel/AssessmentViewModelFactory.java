package com.example.mpproject.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.domain.repository.AssessmentRepository;
import com.example.mpproject.domain.repository.CalendarEventRepository;

// [ViewModel] Factory for AssessmentViewModel.
public class AssessmentViewModelFactory implements ViewModelProvider.Factory {

    private final AssessmentRepository assessmentRepo;
    private final CalendarEventRepository calendarEventRepo;
    private final String userId;
    private final String moduleId;

    public AssessmentViewModelFactory(AssessmentRepository assessmentRepo,
                                      CalendarEventRepository calendarEventRepo,
                                      String userId,
                                      String moduleId) {
        this.assessmentRepo    = assessmentRepo;
        this.calendarEventRepo = calendarEventRepo;
        this.userId            = userId;
        this.moduleId          = moduleId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AssessmentViewModel(assessmentRepo, calendarEventRepo, userId, moduleId);
    }
}
