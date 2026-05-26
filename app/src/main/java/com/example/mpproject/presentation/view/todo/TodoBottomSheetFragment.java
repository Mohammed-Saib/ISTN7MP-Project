package com.example.mpproject.presentation.view.todo;

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

import com.example.mpproject.databinding.FragmentTodoBottomSheetBinding;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.presentation.notification.ReminderScheduler;
import com.example.mpproject.presentation.viewmodel.TaskViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// [View] Bottom sheet for adding and editing todos. Shares TaskViewModel with TasksFragment.
public class TodoBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentTodoBottomSheetBinding binding;
    private TaskViewModel viewModel;

    private Calendar pickedDate = null;
    private Integer pickedHour  = null;
    private Integer pickedMinute = null;
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
        binding = FragmentTodoBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(TaskViewModel.class);

        viewModel.modules.observe(getViewLifecycleOwner(), this::populateModuleDropdown);

        // Recurrence pattern dropdown
        ArrayAdapter<String> patternAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                RECURRENCE_LABELS
        );
        binding.actvRecurrencePattern.setAdapter(patternAdapter);
        binding.actvRecurrencePattern.setText(RECURRENCE_LABELS[1], false); // default Weekly

        // Toggle recurrence controls visibility
        binding.switchRecurring.setOnCheckedChangeListener((btn, checked) -> {
            int v = checked ? View.VISIBLE : View.GONE;
            binding.layoutRecurrencePattern.setVisibility(v);
            binding.llRecurrenceEnd.setVisibility(v);
        });

        binding.btnPickRecurrenceEnd.setOnClickListener(v -> showRecurrenceEndPicker());

        Todo editing = viewModel.getEditingTodo().getValue();
        if (editing != null) {
            prefillForm(editing);
        }

        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnClearDate.setOnClickListener(v -> clearDate());
        binding.btnPickTime.setOnClickListener(v -> showTimePicker());
        binding.btnSave.setOnClickListener(v -> saveTask());

        binding.btnDelete.setOnClickListener(v -> {
            Todo ev = viewModel.getEditingTodo().getValue();
            if (ev == null) return;

            if (ev.getRecurrenceGroupId() != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Task")
                        .setMessage("Delete just this occurrence, or all occurrences of this recurring task?")
                        .setPositiveButton("This one", (d, w) -> {
                            ReminderScheduler.cancelTaskReminder(requireContext(), ev.getTodoId());
                            viewModel.deleteTask(ev);
                            dismiss();
                        })
                        .setNeutralButton("All occurrences", (d, w) -> {
                            /*
                             * This cancels the reminder for the currently selected task.
                             * If you later schedule notifications for every occurrence in a recurring group,
                             * then TaskViewModel/deleteTaskGroup should also cancel each occurrence reminder.
                             */
                            ReminderScheduler.cancelTaskReminder(requireContext(), ev.getTodoId());
                            viewModel.deleteTaskGroup(ev);
                            dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Task")
                        .setMessage("Delete \"" + ev.getTitle() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            ReminderScheduler.cancelTaskReminder(requireContext(), ev.getTodoId());
                            viewModel.deleteTask(ev);
                            dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void prefillForm(Todo todo) {
        binding.tvSheetTitle.setText("Edit Task");
        binding.btnSave.setText("Update Task");
        binding.btnDelete.setVisibility(View.VISIBLE);

        binding.etTitle.setText(todo.getTitle());
        binding.etDescription.setText(todo.getDescription());

        switch (todo.getPriority() != null ? todo.getPriority() : "MEDIUM") {
            case "HIGH":
                binding.chipPriorityHigh.setChecked(true);
                break;

            case "LOW":
                binding.chipPriorityLow.setChecked(true);
                break;

            default:
                binding.chipPriorityMedium.setChecked(true);
                break;
        }

        if (todo.getDueDate() != null) {
            pickedDate = Calendar.getInstance();
            pickedDate.setTimeInMillis(todo.getDueDate());

            Calendar c = pickedDate;
            if (c.get(Calendar.HOUR_OF_DAY) != 0 || c.get(Calendar.MINUTE) != 0) {
                pickedHour   = c.get(Calendar.HOUR_OF_DAY);
                pickedMinute = c.get(Calendar.MINUTE);
            }

            updateDateButton();
        }

        if (todo.getRecurrenceGroupId() != null) {
            // Show info label; recurrence switch is read-only in edit mode
            binding.tvRecurringInfo.setVisibility(View.VISIBLE);
            binding.switchRecurring.setChecked(true);
            binding.switchRecurring.setEnabled(false);
            binding.layoutRecurrencePattern.setVisibility(View.VISIBLE);
            binding.llRecurrenceEnd.setVisibility(View.VISIBLE);

            // Pre-select pattern
            String pattern = todo.getRecurrencePattern();
            if (pattern != null) {
                for (int i = 0; i < RECURRENCE_PATTERNS.length; i++) {
                    if (RECURRENCE_PATTERNS[i].equals(pattern)) {
                        binding.actvRecurrencePattern.setText(RECURRENCE_LABELS[i], false);
                        break;
                    }
                }
            }

            if (todo.getRecurrenceEndDate() != null) {
                recurrenceEndDate = Calendar.getInstance();
                recurrenceEndDate.setTimeInMillis(todo.getRecurrenceEndDate());
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

        Todo editing = viewModel.getEditingTodo().getValue();
        if (editing != null && editing.getModuleId() != null) {
            int idx = moduleIds.indexOf(editing.getModuleId());
            if (idx >= 0) {
                binding.actvModule.setText(moduleNames.get(idx), false);
            }
        }
    }

    private void showDatePicker() {
        Calendar init = pickedDate != null ? pickedDate : Calendar.getInstance();

        new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    pickedDate = Calendar.getInstance();
                    pickedDate.set(year, month, day, 0, 0, 0);
                    pickedDate.set(Calendar.MILLISECOND, 0);
                    updateDateButton();
                },
                init.get(Calendar.YEAR),
                init.get(Calendar.MONTH),
                init.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void clearDate() {
        pickedDate   = null;
        pickedHour   = null;
        pickedMinute = null;

        binding.btnPickDate.setText("No date");
        binding.btnClearDate.setVisibility(View.GONE);
        binding.llTimeRow.setVisibility(View.GONE);
        binding.btnPickTime.setText("No time");
    }

    private void showTimePicker() {
        int initH = pickedHour   != null ? pickedHour   : 9;
        int initM = pickedMinute != null ? pickedMinute : 0;

        new TimePickerDialog(
                requireContext(),
                (picker, hour, minute) -> {
                    pickedHour   = hour;
                    pickedMinute = minute;
                    updateTimeButton();
                },
                initH,
                initM,
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
        if (pickedDate == null) return;

        binding.btnPickDate.setText(DATE_FMT.format(pickedDate.getTime()));
        binding.btnClearDate.setVisibility(View.VISIBLE);
        binding.llTimeRow.setVisibility(View.VISIBLE);

        updateTimeButton();
    }

    private void updateTimeButton() {
        if (pickedHour != null) {
            Calendar t = Calendar.getInstance();
            t.set(Calendar.HOUR_OF_DAY, pickedHour);
            t.set(Calendar.MINUTE, pickedMinute != null ? pickedMinute : 0);
            binding.btnPickTime.setText(TIME_FMT.format(t.getTime()));
        } else {
            binding.btnPickTime.setText("No time");
        }
    }

    private void saveTask() {
        String title = binding.etTitle.getText() != null
                ? binding.etTitle.getText().toString().trim()
                : "";

        if (title.isEmpty()) {
            binding.layoutTitle.setError("Title required");
            return;
        }

        binding.layoutTitle.setError(null);

        String priority = resolvePriority();

        Long dueMs = null;
        if (pickedDate != null) {
            Calendar due = (Calendar) pickedDate.clone();

            if (pickedHour != null) {
                due.set(Calendar.HOUR_OF_DAY, pickedHour);
                due.set(Calendar.MINUTE, pickedMinute != null ? pickedMinute : 0);
            }

            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);

            dueMs = due.getTimeInMillis();
        }

        String selectedModuleName = binding.actvModule.getText() != null
                ? binding.actvModule.getText().toString()
                : "";

        int moduleIdx = moduleNames.indexOf(selectedModuleName);
        String moduleId = (moduleIdx > 0 && moduleIdx < moduleIds.size())
                ? moduleIds.get(moduleIdx)
                : null;

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

        Todo editing = viewModel.getEditingTodo().getValue();

        if (editing != null) {
            editing.setTitle(title);
            editing.setDescription(description);
            editing.setPriority(priority);
            editing.setDueDate(dueMs);
            editing.setModuleId(moduleId);

            /*
             * Cancel the old reminder first.
             * This prevents duplicate notifications if the user edits the due date/time.
             */
            ReminderScheduler.cancelTaskReminder(requireContext(), editing.getTodoId());

            viewModel.updateTask(editing);

            /*
             * Schedule the updated reminder.
             * If dueMs is null or in the past, ReminderScheduler will safely ignore it.
             */
            ReminderScheduler.scheduleTaskReminder(
                    requireContext(),
                    editing.getTodoId(),
                    title,
                    dueMs,
                    priority
            );

            Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show();

        } else {
            Todo createdTodo = viewModel.addTask(
                    title,
                    description,
                    priority,
                    dueMs,
                    moduleId,
                    recurrencePattern,
                    recurrenceEndMs
            );

            if (createdTodo != null) {
                ReminderScheduler.scheduleTaskReminder(
                        requireContext(),
                        createdTodo.getTodoId(),
                        createdTodo.getTitle(),
                        createdTodo.getDueDate(),
                        createdTodo.getPriority()
                );
            }

            Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }

    private String resolvePriority() {
        if (binding.chipPriorityHigh.isChecked()) return "HIGH";
        if (binding.chipPriorityLow.isChecked())  return "LOW";
        return "MEDIUM";
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
        viewModel.setEditingTodo(null);
        binding = null;
    }
}