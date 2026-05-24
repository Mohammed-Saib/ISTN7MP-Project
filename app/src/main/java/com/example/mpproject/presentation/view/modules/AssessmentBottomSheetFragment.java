package com.example.mpproject.presentation.view.modules;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.databinding.FragmentAssessmentBottomSheetBinding;
import com.example.mpproject.domain.model.Assessment;
import com.example.mpproject.presentation.viewmodel.AssessmentViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// [View] Add/edit assessment bottom sheet; shares AssessmentViewModel with ModuleDetailFragment.
public class AssessmentBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentAssessmentBottomSheetBinding binding;
    private AssessmentViewModel viewModel;

    private Calendar pickedDate = null;
    // -1 = no time picked (all-day); 0+ = hour/minute of the time
    private int pickedHour   = -1;
    private int pickedMinute = -1;

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAssessmentBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(AssessmentViewModel.class);

        // Score mode toggle
        binding.toggleScoreMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean isRaw = checkedId == binding.btnModeRaw.getId();
            binding.layoutScorePercentage.setVisibility(isRaw ? View.GONE : View.VISIBLE);
            binding.llRawScore.setVisibility(isRaw ? View.VISIBLE : View.GONE);
        });

        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickTime.setOnClickListener(v -> showTimePicker());
        binding.btnClearTime.setOnClickListener(v -> clearTime());

        // Observe weight warning from ViewModel
        viewModel.totalWeight.observe(getViewLifecycleOwner(), total -> {
            if (total == null) return;
            binding.tvWeightWarning.setVisibility(total > 100.001 ? View.VISIBLE : View.GONE);
        });

        // Pre-fill if editing
        Assessment editing = viewModel.getEditingAssessment().getValue();
        if (editing != null) prefillForm(editing);

        binding.btnSave.setOnClickListener(v -> saveAssessment());

        binding.btnDelete.setOnClickListener(v -> {
            Assessment a = viewModel.getEditingAssessment().getValue();
            if (a == null) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Assessment")
                    .setMessage("Delete \"" + a.getTitle() + "\"? This will also remove any linked calendar event.")
                    .setPositiveButton("Delete", (d, w) -> {
                        viewModel.deleteAssessment(a);
                        Toast.makeText(getContext(), "Assessment deleted", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void prefillForm(Assessment a) {
        binding.tvSheetTitle.setText("Edit Assessment");
        binding.btnSave.setText("Update Assessment");
        binding.btnDelete.setVisibility(View.VISIBLE);

        binding.etTitle.setText(a.getTitle());

        if (a.getAssessmentType() != null) binding.etType.setText(a.getAssessmentType());

        binding.etWeighting.setText(String.format(Locale.getDefault(), "%.0f", a.getWeightingPercent()));

        // Score mode + values
        if ("RAW".equals(a.getScoreMode())) {
            binding.toggleScoreMode.check(binding.btnModeRaw.getId());
            binding.layoutScorePercentage.setVisibility(View.GONE);
            binding.llRawScore.setVisibility(View.VISIBLE);
            if (a.getScoreAchieved() != null)
                binding.etScoreAchieved.setText(formatDouble(a.getScoreAchieved()));
            if (a.getScoreMaximum() != null)
                binding.etScoreMaximum.setText(formatDouble(a.getScoreMaximum()));
        } else {
            binding.toggleScoreMode.check(binding.btnModePercentage.getId());
            if (a.getScoreAchieved() != null)
                binding.etScorePercentage.setText(formatDouble(a.getScoreAchieved()));
        }

        // Date & time
        if (a.getDueDate() != null) {
            pickedDate = Calendar.getInstance();
            pickedDate.setTimeInMillis(a.getDueDate());

            // Check if a real time is stored (not midnight) by looking at h/m
            int h = pickedDate.get(Calendar.HOUR_OF_DAY);
            int m = pickedDate.get(Calendar.MINUTE);
            // Treat midnight as "all-day" unless seconds != 0 (convention)
            boolean hasTime = h != 0 || m != 0;
            if (hasTime) {
                pickedHour   = h;
                pickedMinute = m;
            }

            binding.btnPickDate.setText(DATE_FMT.format(pickedDate.getTime()));
            binding.llTimeRow.setVisibility(View.VISIBLE);
            if (hasTime) {
                updateTimeButton();
                binding.btnClearTime.setVisibility(View.VISIBLE);
            }
        }

        if (a.getNotes() != null) binding.etNotes.setText(a.getNotes());
    }

    private void showDatePicker() {
        Calendar init = pickedDate != null ? pickedDate : Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (picker, year, month, day) -> {
                    pickedDate = Calendar.getInstance();
                    pickedDate.set(year, month, day, 0, 0, 0);
                    pickedDate.set(Calendar.MILLISECOND, 0);
                    binding.btnPickDate.setText(DATE_FMT.format(pickedDate.getTime()));
                    binding.llTimeRow.setVisibility(View.VISIBLE);
                },
                init.get(Calendar.YEAR),
                init.get(Calendar.MONTH),
                init.get(Calendar.DAY_OF_MONTH));

        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "Clear date", (d, w) -> {
            pickedDate   = null;
            pickedHour   = -1;
            pickedMinute = -1;
            binding.btnPickDate.setText("No date set");
            binding.llTimeRow.setVisibility(View.GONE);
            binding.btnPickTime.setText("Add time (optional)");
            binding.btnClearTime.setVisibility(View.GONE);
        });

        dialog.show();
    }

    private void showTimePicker() {
        int initH = pickedHour >= 0 ? pickedHour : 9;
        int initM = pickedMinute >= 0 ? pickedMinute : 0;
        new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
            pickedHour   = hour;
            pickedMinute = minute;
            updateTimeButton();
            binding.btnClearTime.setVisibility(View.VISIBLE);
        }, initH, initM, true).show();
    }

    private void clearTime() {
        pickedHour   = -1;
        pickedMinute = -1;
        binding.btnPickTime.setText("Add time (optional)");
        binding.btnClearTime.setVisibility(View.GONE);
    }

    private void updateTimeButton() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, pickedHour);
        c.set(Calendar.MINUTE, pickedMinute);
        binding.btnPickTime.setText(TIME_FMT.format(c.getTime()));
    }

    private void saveAssessment() {
        String title = binding.etTitle.getText() != null
                ? binding.etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            binding.layoutTitle.setError("Title required");
            return;
        }
        binding.layoutTitle.setError(null);

        String type = binding.etType.getText() != null
                ? binding.etType.getText().toString().trim() : "";
        if (type.isEmpty()) type = null;

        String weightingStr = binding.etWeighting.getText() != null
                ? binding.etWeighting.getText().toString().trim() : "";
        double weighting = 0;
        if (!weightingStr.isEmpty()) {
            try { weighting = Double.parseDouble(weightingStr); }
            catch (NumberFormatException ignored) {}
        }
        if (weighting < 0) weighting = 0;

        // Validate total weighting won't exceed 100%
        Assessment editingForWeight = viewModel.getEditingAssessment().getValue();
        double existingWeight = editingForWeight != null ? editingForWeight.getWeightingPercent() : 0;
        Double totalWeightVal = viewModel.totalWeight.getValue();
        double currentTotal = totalWeightVal != null ? totalWeightVal : 0;
        double projected = currentTotal - existingWeight + weighting;
        if (projected > 100.001) {
            double remaining = 100 - (currentTotal - existingWeight);
            binding.layoutWeighting.setError(String.format(Locale.getDefault(),
                    "Max %.0f%% remaining (total would be %.0f%%)", remaining, projected));
            return;
        }
        binding.layoutWeighting.setError(null);

        boolean isRaw = binding.toggleScoreMode.getCheckedButtonId() == binding.btnModeRaw.getId();
        String scoreMode = isRaw ? "RAW" : "PERCENTAGE";

        Double scoreAchieved = null, scoreMaximum = null;
        if (isRaw) {
            String sa = binding.etScoreAchieved.getText() != null
                    ? binding.etScoreAchieved.getText().toString().trim() : "";
            String sm = binding.etScoreMaximum.getText() != null
                    ? binding.etScoreMaximum.getText().toString().trim() : "";
            if (!sa.isEmpty()) try { scoreAchieved = Double.parseDouble(sa); } catch (NumberFormatException ignored) {}
            if (!sm.isEmpty()) try { scoreMaximum  = Double.parseDouble(sm); } catch (NumberFormatException ignored) {}
        } else {
            String sp = binding.etScorePercentage.getText() != null
                    ? binding.etScorePercentage.getText().toString().trim() : "";
            if (!sp.isEmpty()) try { scoreAchieved = Double.parseDouble(sp); } catch (NumberFormatException ignored) {}
        }

        // Compute dueDate timestamp; incorporate time if the user picked one
        Long dueDate = null;
        boolean isAllDay = true;
        if (pickedDate != null) {
            if (pickedHour >= 0) {
                pickedDate.set(Calendar.HOUR_OF_DAY, pickedHour);
                pickedDate.set(Calendar.MINUTE, pickedMinute);
                pickedDate.set(Calendar.SECOND, 0);
                isAllDay = false;
            } else {
                pickedDate.set(Calendar.HOUR_OF_DAY, 0);
                pickedDate.set(Calendar.MINUTE, 0);
                pickedDate.set(Calendar.SECOND, 0);
            }
            dueDate = pickedDate.getTimeInMillis();
        }

        String notes = binding.etNotes.getText() != null
                ? binding.etNotes.getText().toString().trim() : null;
        if (notes != null && notes.isEmpty()) notes = null;

        Assessment editing = viewModel.getEditingAssessment().getValue();
        if (editing != null) {
            viewModel.updateAssessment(editing, title, type, weighting, scoreMode,
                    scoreAchieved, scoreMaximum, dueDate, isAllDay, notes);
            Toast.makeText(getContext(), "Assessment updated", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.addAssessment(title, type, weighting, scoreMode,
                    scoreAchieved, scoreMaximum, dueDate, isAllDay, notes);
            Toast.makeText(getContext(), "Assessment saved", Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }

    private String formatDouble(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.setEditingAssessment(null);
        binding = null;
    }
}
