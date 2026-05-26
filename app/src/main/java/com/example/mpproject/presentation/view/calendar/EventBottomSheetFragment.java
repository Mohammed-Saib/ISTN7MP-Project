package com.example.mpproject.presentation.view.calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.AssessmentRepositoryImpl;
import com.example.mpproject.databinding.FragmentEventBottomSheetBinding;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.presentation.notification.ReminderScheduler;
import com.example.mpproject.presentation.viewmodel.CalendarViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// [View] BottomSheetDialogFragment for creating and editing CalendarEvents.
// Shares CalendarViewModel with the parent CalendarFragment via requireParentFragment().
public class EventBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentEventBottomSheetBinding binding;
    private CalendarViewModel viewModel;

    private Calendar pickedDate;
    private int startHour   = 9,  startMinute = 0;
    private int endHour     = 10, endMinute   = 0;
    private boolean hasEndTime = false;
    private Calendar recurrenceEndDate = null;

    private final List<String> moduleNames = new ArrayList<>();
    private final List<String> moduleIds   = new ArrayList<>();

    private static final String[] RECURRENCE_LABELS   = {"Daily", "Weekly", "Monthly", "Yearly"};
    private static final String[] RECURRENCE_PATTERNS = {"DAILY", "WEEKLY", "MONTHLY", "YEARLY"};

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEventBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(CalendarViewModel.class);

        Calendar sel = viewModel.getSelectedDay().getValue();
        pickedDate = sel != null ? (Calendar) sel.clone() : Calendar.getInstance();

        updateDateButton();
        updateTimeButtons();

        viewModel.modules.observe(getViewLifecycleOwner(), this::populateModuleDropdown);

        CalendarEvent editing = viewModel.getEditingEvent().getValue();
        if (editing != null) {
            prefillForm(editing);
        }

        ArrayAdapter<String> patternAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                RECURRENCE_LABELS
        );
        binding.actvRecurrencePattern.setAdapter(patternAdapter);
        binding.actvRecurrencePattern.setText(RECURRENCE_LABELS[1], false);

        binding.switchRecurring.setOnCheckedChangeListener((btn, checked) -> {
            int v = checked ? View.VISIBLE : View.GONE;
            binding.layoutRecurrencePattern.setVisibility(v);
            binding.llRecurrenceEnd.setVisibility(v);
        });

        binding.btnPickRecurrenceEnd.setOnClickListener(v -> showRecurrenceEndPicker());

        binding.switchAllDay.setOnCheckedChangeListener((btn, checked) ->
                binding.llTimePickers.setVisibility(checked ? View.GONE : View.VISIBLE));

        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickStartTime.setOnClickListener(v -> showTimePicker(true));
        binding.btnPickEndTime.setOnClickListener(v -> showTimePicker(false));

        binding.btnClearEndTime.setOnClickListener(v -> {
            hasEndTime = false;
            updateTimeButtons();
        });

        binding.btnSave.setOnClickListener(v -> saveEvent());

        binding.btnDelete.setOnClickListener(v -> {
            CalendarEvent ev = viewModel.getEditingEvent().getValue();
            if (ev == null) return;

            if (ev.getLinkedAssessmentId() != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Assessment Event")
                        .setMessage("This event is linked to an assessment. Deleting it removes it from your calendar, but the assessment record remains.")
                        .setPositiveButton("Delete from calendar", (d, w) -> {
                            AppDatabase db = AppDatabase.getDatabase(requireContext());
                            new AssessmentRepositoryImpl(db.assessmentDao())
                                    .clearCalendarEventId(ev.getLinkedAssessmentId());

                            ReminderScheduler.cancelEventReminder(requireContext(), ev.getEventId());
                            viewModel.deleteEvent(ev);
                            dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

            } else if (ev.getRecurrenceGroupId() != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Event")
                        .setMessage("Delete just this occurrence, or all occurrences of this recurring event?")
                        .setPositiveButton("This one", (d, w) -> {
                            ReminderScheduler.cancelEventReminder(requireContext(), ev.getEventId());
                            viewModel.deleteEvent(ev);
                            dismiss();
                        })
                        .setNeutralButton("All occurrences", (d, w) -> {
                            /*
                             * This cancels the selected event reminder.
                             * Since the current implementation only schedules the first created recurring event,
                             * this is enough when deleting from that first event. If you later schedule every
                             * recurring occurrence, cancellation should happen inside deleteEventGroup too.
                             */
                            ReminderScheduler.cancelEventReminder(requireContext(), ev.getEventId());
                            viewModel.deleteEventGroup(ev);
                            dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Event")
                        .setMessage("Are you sure you want to delete \"" + ev.getTitle() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            ReminderScheduler.cancelEventReminder(requireContext(), ev.getEventId());
                            viewModel.deleteEvent(ev);
                            dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void prefillForm(CalendarEvent e) {
        binding.tvSheetTitle.setText("Edit Event");
        binding.btnSave.setText("Update Event");
        binding.btnDelete.setVisibility(View.VISIBLE);

        if (e.getLinkedAssessmentId() != null) {
            binding.tvTypeLabel.setVisibility(View.GONE);
            binding.chipGroupType.setVisibility(View.GONE);
            binding.tvTypeAssessment.setVisibility(View.VISIBLE);
            binding.layoutModule.setEnabled(false);
            binding.actvModule.setFocusable(false);
            binding.actvModule.setClickable(false);
        }

        binding.etTitle.setText(e.getTitle());
        binding.etDescription.setText(e.getDescription());
        binding.switchAllDay.setChecked(e.isAllDay());
        binding.llTimePickers.setVisibility(e.isAllDay() ? View.GONE : View.VISIBLE);

        pickedDate = Calendar.getInstance();
        pickedDate.setTimeInMillis(e.getStartTime());
        updateDateButton();

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(e.getStartTime());
        startHour   = start.get(Calendar.HOUR_OF_DAY);
        startMinute = start.get(Calendar.MINUTE);

        if (e.getEndTime() != null) {
            Calendar end = Calendar.getInstance();
            end.setTimeInMillis(e.getEndTime());
            endHour    = end.get(Calendar.HOUR_OF_DAY);
            endMinute  = end.get(Calendar.MINUTE);
            hasEndTime = true;
        }

        updateTimeButtons();

        switch (e.getType() != null ? e.getType() : "") {
            case "EXAM":
                binding.chipTypeExam.setChecked(true);
                break;

            case "ASSIGNMENT_DUE":
                binding.chipTypeAssignment.setChecked(true);
                break;

            case "PERSONAL":
                binding.chipTypePersonal.setChecked(true);
                break;

            default:
                binding.chipTypeLecture.setChecked(true);
                break;
        }

        if (e.getRecurrenceGroupId() != null) {
            binding.tvRecurringInfo.setVisibility(View.VISIBLE);
            binding.switchRecurring.setChecked(true);
            binding.switchRecurring.setEnabled(false);
            binding.layoutRecurrencePattern.setVisibility(View.VISIBLE);
            binding.llRecurrenceEnd.setVisibility(View.VISIBLE);

            String pattern = e.getRecurrencePattern();
            if (pattern != null) {
                for (int i = 0; i < RECURRENCE_PATTERNS.length; i++) {
                    if (RECURRENCE_PATTERNS[i].equals(pattern)) {
                        binding.actvRecurrencePattern.setText(RECURRENCE_LABELS[i], false);
                        break;
                    }
                }
            }

            if (e.getRecurrenceEndDate() != null) {
                recurrenceEndDate = Calendar.getInstance();
                recurrenceEndDate.setTimeInMillis(e.getRecurrenceEndDate());
                binding.btnPickRecurrenceEnd.setText(DATE_FMT.format(recurrenceEndDate.getTime()));
            }
        }
    }

    private void populateModuleDropdown(List<Module> modules) {
        moduleNames.clear();
        moduleIds.clear();

        moduleNames.add("None");
        moduleIds.add(null);

        if (modules != null) {
            for (Module m : modules) {
                moduleNames.add(
                        m.getName()
                                + (m.getModuleCode() != null ? " (" + m.getModuleCode() + ")" : "")
                );
                moduleIds.add(m.getModuleId());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                moduleNames
        );
        binding.actvModule.setAdapter(adapter);

        CalendarEvent editing = viewModel.getEditingEvent().getValue();
        if (editing != null && editing.getModuleId() != null) {
            int idx = moduleIds.indexOf(editing.getModuleId());
            if (idx >= 0) {
                binding.actvModule.setText(moduleNames.get(idx), false);
            }
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    pickedDate.set(Calendar.YEAR, year);
                    pickedDate.set(Calendar.MONTH, month);
                    pickedDate.set(Calendar.DAY_OF_MONTH, day);
                    updateDateButton();
                },
                pickedDate.get(Calendar.YEAR),
                pickedDate.get(Calendar.MONTH),
                pickedDate.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker(boolean isStart) {
        int initHour   = isStart ? startHour   : endHour;
        int initMinute = isStart ? startMinute : endMinute;

        new TimePickerDialog(
                requireContext(),
                (picker, hour, minute) -> {
                    if (isStart) {
                        startHour = hour;
                        startMinute = minute;
                    } else {
                        endHour = hour;
                        endMinute = minute;
                        hasEndTime = true;
                    }

                    updateTimeButtons();
                },
                initHour,
                initMinute,
                true
        ).show();
    }

    private void showRecurrenceEndPicker() {
        Calendar init = recurrenceEndDate != null ? recurrenceEndDate : Calendar.getInstance();

        new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    recurrenceEndDate = Calendar.getInstance();
                    recurrenceEndDate.set(year, month, day, 23, 59, 59);
                    recurrenceEndDate.set(Calendar.MILLISECOND, 0);
                    binding.btnPickRecurrenceEnd.setText(DATE_FMT.format(recurrenceEndDate.getTime()));
                },
                init.get(Calendar.YEAR),
                init.get(Calendar.MONTH),
                init.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateButton() {
        binding.btnPickDate.setText(DATE_FMT.format(pickedDate.getTime()));
    }

    private void updateTimeButtons() {
        binding.btnPickStartTime.setText(formatTime(startHour, startMinute));
        binding.btnPickEndTime.setText(hasEndTime ? formatTime(endHour, endMinute) : "No end");
        binding.btnClearEndTime.setVisibility(hasEndTime ? View.VISIBLE : View.GONE);
    }

    private String formatTime(int h, int m) {
        return TIME_FMT.format(new Date(0L + h * 3600000L + m * 60000L));
    }

    private void saveEvent() {
        String title = binding.etTitle.getText() != null
                ? binding.etTitle.getText().toString().trim()
                : "";

        if (title.isEmpty()) {
            binding.layoutTitle.setError("Title required");
            return;
        }

        binding.layoutTitle.setError(null);

        CalendarEvent editing = viewModel.getEditingEvent().getValue();
        boolean isAssessmentEvent = editing != null && editing.getLinkedAssessmentId() != null;

        String type;
        String moduleId;

        if (isAssessmentEvent) {
            type = editing.getType();
            moduleId = editing.getModuleId();
        } else {
            type = resolveType();

            String selectedModuleName = binding.actvModule.getText() != null
                    ? binding.actvModule.getText().toString()
                    : "";

            int moduleIdx = moduleNames.indexOf(selectedModuleName);
            moduleId = (moduleIdx > 0 && moduleIdx < moduleIds.size())
                    ? moduleIds.get(moduleIdx)
                    : null;
        }

        Calendar startCal = (Calendar) pickedDate.clone();
        boolean allDay = binding.switchAllDay.isChecked();

        if (!allDay) {
            startCal.set(Calendar.HOUR_OF_DAY, startHour);
            startCal.set(Calendar.MINUTE, startMinute);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        } else {
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        }

        long startMs = startCal.getTimeInMillis();

        Long endMs = null;
        if (!allDay && hasEndTime) {
            Calendar endCal = (Calendar) pickedDate.clone();
            endCal.set(Calendar.HOUR_OF_DAY, endHour);
            endCal.set(Calendar.MINUTE, endMinute);
            endCal.set(Calendar.SECOND, 0);
            endCal.set(Calendar.MILLISECOND, 0);

            endMs = endCal.getTimeInMillis();

            if (endMs <= startMs) {
                endMs = startMs + 3600000L;
            }
        }

        String description = binding.etDescription.getText() != null
                ? binding.etDescription.getText().toString().trim()
                : null;

        if (description != null && description.isEmpty()) {
            description = null;
        }

        String recurrencePattern = null;
        Long recurrenceEndMs = null;

        if (binding.switchRecurring.isChecked()) {
            recurrencePattern = resolveRecurrencePattern();
            recurrenceEndMs = recurrenceEndDate != null
                    ? recurrenceEndDate.getTimeInMillis()
                    : null;
        }

        if (editing != null) {
            editing.setTitle(title);
            editing.setType(type);
            editing.setStartTime(startMs);
            editing.setEndTime(endMs);
            editing.setAllDay(allDay);
            editing.setModuleId(moduleId);
            editing.setDescription(description);

            ReminderScheduler.cancelEventReminder(requireContext(), editing.getEventId());

            viewModel.updateEvent(editing);

            ReminderScheduler.scheduleEventReminder(
                    requireContext(),
                    editing.getEventId(),
                    title,
                    startMs,
                    allDay,
                    type
            );

            if (isAssessmentEvent) {
                AppDatabase db = AppDatabase.getDatabase(requireContext());
                new AssessmentRepositoryImpl(db.assessmentDao())
                        .updateDueDateAndNotes(editing.getLinkedAssessmentId(), startMs, description);
            }

            Toast.makeText(getContext(), "Event updated", Toast.LENGTH_SHORT).show();

        } else {
            CalendarEvent createdEvent = viewModel.addEvent(
                    title,
                    type,
                    startMs,
                    endMs,
                    allDay,
                    moduleId,
                    description,
                    recurrencePattern,
                    recurrenceEndMs
            );

            if (createdEvent != null) {
                ReminderScheduler.scheduleEventReminder(
                        requireContext(),
                        createdEvent.getEventId(),
                        createdEvent.getTitle(),
                        createdEvent.getStartTime(),
                        createdEvent.isAllDay(),
                        createdEvent.getType()
                );
            }

            Toast.makeText(getContext(), "Event added", Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }

    private String resolveType() {
        if (binding.chipTypeExam.isChecked()) return "EXAM";
        if (binding.chipTypeAssignment.isChecked()) return "ASSIGNMENT_DUE";
        if (binding.chipTypePersonal.isChecked()) return "PERSONAL";
        return "LECTURE";
    }

    private String resolveRecurrencePattern() {
        String label = binding.actvRecurrencePattern.getText() != null
                ? binding.actvRecurrencePattern.getText().toString()
                : "";

        for (int i = 0; i < RECURRENCE_LABELS.length; i++) {
            if (RECURRENCE_LABELS[i].equals(label)) {
                return RECURRENCE_PATTERNS[i];
            }
        }

        return "WEEKLY";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.setEditingEvent(null);
        binding = null;
    }
}