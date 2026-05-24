package com.example.mpproject.presentation.view.modules;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.mpproject.R;
import com.example.mpproject.databinding.FragmentModuleBottomSheetBinding;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.presentation.viewmodel.ModuleViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

// [View] Bottom sheet for adding and editing modules. Shares ModuleViewModel with ModulesFragment.
public class ModuleBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentModuleBottomSheetBinding binding;
    private ModuleViewModel viewModel;

    private static final String[] SEMESTER_OPTIONS = {"Semester 1", "Semester 2", "Other"};

    // hex strings matching the module accent palette in colors.xml (light variants)
    private static final String[] COLOR_HEX = {
            "#4F46E5", // indigo
            "#E11D48", // rose
            "#D97706", // amber
            "#0D9488", // teal
            "#7C3AED", // violet
            "#16A34A", // forest
            "#EA580C"  // ember
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModuleBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(ModuleViewModel.class);

        // Populate semester dropdown
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                SEMESTER_OPTIONS);
        binding.actvSemester.setAdapter(semesterAdapter);

        // Show/hide custom semester field based on selection
        binding.actvSemester.setOnItemClickListener((parent, v, position, id) -> {
            boolean isOther = SEMESTER_OPTIONS[position].equals("Other");
            binding.layoutSemesterCustom.setVisibility(isOther ? View.VISIBLE : View.GONE);
        });

        Module editing = viewModel.getEditingModule().getValue();
        if (editing != null) prefillForm(editing);

        binding.btnSave.setOnClickListener(v -> saveModule());

        binding.btnArchive.setOnClickListener(v -> {
            Module m = viewModel.getEditingModule().getValue();
            if (m == null) return;
            if (m.isArchived()) {
                viewModel.unarchiveModule(m);
                Toast.makeText(getContext(), "Module restored", Toast.LENGTH_SHORT).show();
            } else {
                viewModel.archiveModule(m);
                Toast.makeText(getContext(), "Module archived", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });

        binding.btnDelete.setOnClickListener(v -> {
            Module m = viewModel.getEditingModule().getValue();
            if (m == null) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Module")
                    .setMessage("Delete \"" + m.getName() + "\"? This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        viewModel.deleteModule(m);
                        dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void prefillForm(Module m) {
        binding.tvSheetTitle.setText("Edit Module");
        binding.btnSave.setText("Update Module");
        binding.btnArchive.setVisibility(View.VISIBLE);
        binding.btnDelete.setVisibility(View.VISIBLE);

        binding.etName.setText(m.getName());
        binding.etCode.setText(m.getModuleCode());
        prefillSemester(m.getSemester());
        binding.etLecturerName.setText(m.getLecturerName());
        binding.etLecturerEmail.setText(m.getLecturerEmail());
        binding.etOfficeHours.setText(m.getLecturerOfficeHours());

        // Archive button label depends on current state
        binding.btnArchive.setText(m.isArchived() ? "Restore Module" : "Archive Module");

        // Pre-select color chip
        selectColorChip(m.getColor());
    }

    private void prefillSemester(String semester) {
        if (semester == null || semester.isEmpty()) return;
        boolean isStandard = semester.equals("Semester 1") || semester.equals("Semester 2");
        if (isStandard) {
            binding.actvSemester.setText(semester, false);
        } else {
            binding.actvSemester.setText("Other", false);
            binding.layoutSemesterCustom.setVisibility(View.VISIBLE);
            binding.etSemesterCustom.setText(semester);
        }
    }

    private void selectColorChip(String hex) {
        if (hex == null) return;
        int[] chipIds = {
                R.id.chip_color_indigo, R.id.chip_color_rose, R.id.chip_color_amber,
                R.id.chip_color_teal, R.id.chip_color_violet, R.id.chip_color_forest,
                R.id.chip_color_ember
        };
        for (int i = 0; i < COLOR_HEX.length; i++) {
            if (COLOR_HEX[i].equalsIgnoreCase(hex)) {
                ((com.google.android.material.chip.Chip)
                        binding.getRoot().findViewById(chipIds[i])).setChecked(true);
                return;
            }
        }
    }

    private String resolveSelectedColor() {
        int[] chipIds = {
                R.id.chip_color_indigo, R.id.chip_color_rose, R.id.chip_color_amber,
                R.id.chip_color_teal, R.id.chip_color_violet, R.id.chip_color_forest,
                R.id.chip_color_ember
        };
        for (int i = 0; i < chipIds.length; i++) {
            com.google.android.material.chip.Chip chip =
                    binding.getRoot().findViewById(chipIds[i]);
            if (chip.isChecked()) return COLOR_HEX[i];
        }
        return COLOR_HEX[0]; // fallback indigo
    }

    private void saveModule() {
        String name = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            binding.layoutName.setError("Module name required");
            return;
        }
        binding.layoutName.setError(null);

        String code        = trimOrNull(binding.etCode.getText());
        String semester    = resolveSemester();
        String lecName     = trimOrNull(binding.etLecturerName.getText());
        String lecEmail    = trimOrNull(binding.etLecturerEmail.getText());
        String officeHours = trimOrNull(binding.etOfficeHours.getText());
        String color       = resolveSelectedColor();

        if (!TextUtils.isEmpty(lecEmail) && !Patterns.EMAIL_ADDRESS.matcher(lecEmail).matches()) {
            binding.layoutLecturerEmail.setError("Invalid email address");
            return;
        }
        binding.layoutLecturerEmail.setError(null);

        Module editing = viewModel.getEditingModule().getValue();
        if (editing != null) {
            editing.setName(name);
            editing.setModuleCode(code);
            editing.setSemester(semester);
            editing.setColor(color);
            editing.setLecturerName(lecName);
            editing.setLecturerEmail(lecEmail);
            editing.setLecturerOfficeHours(officeHours);
            viewModel.updateModule(editing);
            Toast.makeText(getContext(), "Module updated", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.addModule(name, code, color, semester, lecName, lecEmail, officeHours);
            Toast.makeText(getContext(), "Module added", Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }

    private String resolveSemester() {
        String selected = binding.actvSemester.getText() != null
                ? binding.actvSemester.getText().toString().trim() : "";
        if (selected.equals("Other")) {
            return trimOrNull(binding.etSemesterCustom.getText());
        }
        return selected.isEmpty() ? null : selected;
    }

    private String trimOrNull(android.text.Editable e) {
        if (e == null) return null;
        String s = e.toString().trim();
        return s.isEmpty() ? null : s;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.setEditingModule(null);
        binding = null;
    }
}
