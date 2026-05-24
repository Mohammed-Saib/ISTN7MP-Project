package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.model.Assessment;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.repository.AssessmentRepository;
import com.example.mpproject.domain.repository.CalendarEventRepository;

import java.util.List;
import java.util.UUID;

// [ViewModel] Drives ModuleDetailFragment and AssessmentBottomSheetFragment.
public class AssessmentViewModel extends ViewModel {

    private final AssessmentRepository assessmentRepo;
    private final CalendarEventRepository calendarEventRepo;
    private final String userId;
    private final String moduleId;

    public final LiveData<List<Assessment>> assessments;

    // Derived grade stats — totalWeight drives the single source observer; the other two
    // are plain MutableLiveData updated inside recalcStats() so recalcStats runs once per change.
    public final MediatorLiveData<Double> totalWeight     = new MediatorLiveData<>();
    public final MutableLiveData<Double>  gradedWeight    = new MutableLiveData<>();
    public final MutableLiveData<Double>  weightedAverage = new MutableLiveData<>();

    // The assessment being edited; null = add mode
    private final MutableLiveData<Assessment> editingAssessment = new MutableLiveData<>(null);

    public AssessmentViewModel(AssessmentRepository assessmentRepo,
                               CalendarEventRepository calendarEventRepo,
                               String userId,
                               String moduleId) {
        this.assessmentRepo    = assessmentRepo;
        this.calendarEventRepo = calendarEventRepo;
        this.userId            = userId;
        this.moduleId          = moduleId;

        assessments = assessmentRepo.getByModule(moduleId);

        totalWeight.addSource(assessments, list -> recalcStats(list));
    }

    public LiveData<Assessment> getEditingAssessment() { return editingAssessment; }
    public void setEditingAssessment(Assessment a)     { editingAssessment.setValue(a); }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public void addAssessment(String title, String type, double weighting,
                              String scoreMode, Double scoreAchieved, Double scoreMaximum,
                              Long dueDate, boolean isAllDay, String notes) {
        String assessmentId = UUID.randomUUID().toString();
        Assessment a = new Assessment(assessmentId, moduleId, userId, title);
        a.setAssessmentType(type);
        a.setWeightingPercent(weighting);
        a.setScoreMode(scoreMode);
        a.setScoreAchieved(scoreAchieved);
        a.setScoreMaximum(scoreMaximum);
        a.setDueDate(dueDate);
        a.setNotes(notes);

        if (dueDate != null) {
            String eventId = UUID.randomUUID().toString();
            a.setCalendarEventId(eventId);
            createLinkedEvent(eventId, assessmentId, title, type, dueDate, isAllDay, notes);
        }

        assessmentRepo.insert(a);
    }

    public void updateAssessment(Assessment existing, String title, String type, double weighting,
                                 String scoreMode, Double scoreAchieved, Double scoreMaximum,
                                 Long dueDate, boolean isAllDay, String notes) {
        String oldEventId = existing.getCalendarEventId();

        existing.setTitle(title);
        existing.setAssessmentType(type);
        existing.setWeightingPercent(weighting);
        existing.setScoreMode(scoreMode);
        existing.setScoreAchieved(scoreAchieved);
        existing.setScoreMaximum(scoreMaximum);
        existing.setDueDate(dueDate);
        existing.setNotes(notes);
        existing.setUpdatedAt(System.currentTimeMillis());

        if (dueDate == null) {
            if (oldEventId != null) {
                deleteLinkedEvent(oldEventId);
                existing.setCalendarEventId(null);
            }
        } else if (oldEventId == null) {
            String eventId = UUID.randomUUID().toString();
            existing.setCalendarEventId(eventId);
            createLinkedEvent(eventId, existing.getAssessmentId(), title, type, dueDate, isAllDay, notes);
        } else {
            updateLinkedEventTitle(oldEventId, title, type, dueDate, isAllDay, notes);
        }

        assessmentRepo.update(existing);
    }

    public void deleteAssessment(Assessment assessment) {
        if (assessment.getCalendarEventId() != null) {
            deleteLinkedEvent(assessment.getCalendarEventId());
        }
        assessmentRepo.delete(assessment);
    }

    // ── Calendar event helpers ───────────────────────────────────────────────

    private void createLinkedEvent(String eventId, String assessmentId,
                                   String title, String type, long dueDate, boolean isAllDay,
                                   String notes) {
        CalendarEvent ev = new CalendarEvent(eventId, userId, title, dueDate);
        ev.setType(calendarTypeForAssessmentType(type));
        ev.setLinkedAssessmentId(assessmentId);
        ev.setModuleId(moduleId);
        ev.setAllDay(isAllDay);
        ev.setDescription(notes);
        ev.setUpdatedAt(System.currentTimeMillis());
        calendarEventRepo.insert(ev);
    }

    @SuppressWarnings("unchecked")
    private void updateLinkedEventTitle(String eventId, String title, String type,
                                        long dueDate, boolean isAllDay, String notes) {
        LiveData<CalendarEvent> liveData = calendarEventRepo.getById(eventId);
        // One-shot: remove the observer after the first delivery to avoid accumulating observers.
        Observer<CalendarEvent>[] holder = new Observer[1];
        holder[0] = ev -> {
            liveData.removeObserver(holder[0]);
            if (ev == null) return;
            ev.setTitle(title);
            ev.setType(calendarTypeForAssessmentType(type));
            ev.setStartTime(dueDate);
            ev.setAllDay(isAllDay);
            ev.setDescription(notes);
            ev.setUpdatedAt(System.currentTimeMillis());
            calendarEventRepo.update(ev);
        };
        liveData.observeForever(holder[0]);
    }

    @SuppressWarnings("unchecked")
    private void deleteLinkedEvent(String eventId) {
        LiveData<CalendarEvent> liveData = calendarEventRepo.getById(eventId);
        Observer<CalendarEvent>[] holder = new Observer[1];
        holder[0] = ev -> {
            liveData.removeObserver(holder[0]);
            if (ev != null) calendarEventRepo.delete(ev);
        };
        liveData.observeForever(holder[0]);
    }

    // Maps free-text assessment type to a calendar event type string via keyword matching.
    public static String calendarTypeForAssessmentType(String assessmentType) {
        if (assessmentType == null) return "PERSONAL";
        String lower = assessmentType.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("quiz"))       return "QUIZ";
        if (lower.contains("test"))       return "TEST";
        if (lower.contains("exam"))       return "EXAM";
        if (lower.contains("lab"))        return "LAB";
        if (lower.contains("assignment")
         || lower.contains("homework")
         || lower.contains("project"))    return "ASSIGNMENT_DUE";
        return "PERSONAL";
    }

    // ── Grade computation ────────────────────────────────────────────────────

    private void recalcStats(List<Assessment> list) {
        double total = 0, graded = 0, sumContributions = 0;
        if (list != null) {
            for (Assessment a : list) {
                total += a.getWeightingPercent();
                if (a.isGraded()) {
                    graded += a.getWeightingPercent();
                    Double contrib = a.getContributionPercent();
                    if (contrib != null) sumContributions += contrib;
                }
            }
        }
        totalWeight.setValue(total);
        gradedWeight.setValue(graded);
        // weighted average = sum of (score% × weight) / sum of graded weights × 100
        weightedAverage.setValue(graded > 0 ? sumContributions / graded * 100.0 : null);
    }
}
