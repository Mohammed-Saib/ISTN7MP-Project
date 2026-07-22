package com.example.mpproject.presentation.view.modules;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.mpproject.R;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.AssessmentRepositoryImpl;
import com.example.mpproject.data.repository.CalendarEventRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.databinding.FragmentModuleDetailBinding;
import com.example.mpproject.domain.model.Assessment;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.presentation.adapter.AssessmentAdapter;
import com.example.mpproject.presentation.viewmodel.AssessmentViewModel;
import com.example.mpproject.presentation.viewmodel.AssessmentViewModelFactory;
import com.example.mpproject.presentation.viewmodel.ModuleViewModel;
import com.example.mpproject.presentation.viewmodel.ModuleViewModelFactory;
import com.example.mpproject.data.local.LocalSessionManager;

import java.util.ArrayList;
import java.util.List;

// [View] Module header, assessments search, and assessments list for a single module (receives moduleId nav arg).
public class ModuleDetailFragment extends Fragment {

    private FragmentModuleDetailBinding binding;
    private AssessmentViewModel assessmentViewModel;
    private ModuleViewModel moduleViewModel;
    private AssessmentAdapter adapter;
    private String moduleId;

    private final List<Assessment> fullList = new ArrayList<>();
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModuleDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userId = LocalSessionManager.getCurrentUserId(requireContext());
        if (userId == null) return;

        // Retrieve moduleId from nav args bundle
        if (getArguments() != null) {
            moduleId = getArguments().getString("moduleId");
        }
        if (moduleId == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }

        binding.btnViewModuleNotes.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("moduleId", moduleId);

            Navigation.findNavController(v)
                    .navigate(R.id.action_moduleDetailFragment_to_notesFragment, args);

            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                    requireActivity().findViewById(R.id.bottom_navigation);

            if (bottomNav != null && bottomNav.getMenu().findItem(R.id.notesFragment) != null) {
                bottomNav.getMenu().findItem(R.id.notesFragment).setChecked(true);
            }
        });

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        String uid = userId;

        // Module ViewModel (for header / edit)
        ModuleViewModelFactory moduleFactory = new ModuleViewModelFactory(
                new ModuleRepositoryImpl(db.moduleDao()), uid);
        moduleViewModel = new ViewModelProvider(this, moduleFactory).get(ModuleViewModel.class);

        // Assessment ViewModel
        AssessmentViewModelFactory assessmentFactory = new AssessmentViewModelFactory(
                new AssessmentRepositoryImpl(db.assessmentDao()),
                new CalendarEventRepositoryImpl(db.calendarEventDao()),
                uid, moduleId);
        assessmentViewModel = new ViewModelProvider(this, assessmentFactory)
                .get(AssessmentViewModel.class);

        adapter = new AssessmentAdapter(assessment -> {
            assessmentViewModel.setEditingAssessment(assessment);
            new AssessmentBottomSheetFragment().show(getChildFragmentManager(), "edit_assessment");
        });

        binding.rvAssessments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAssessments.setAdapter(adapter);

        // Toolbar back navigation (no title — module name is shown in the header card below)
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        // Observe module to populate header (auto-pop if module is deleted).
        // Guard: only pop if BOTH lists have delivered their first value — if only one has arrived,
        // the module might simply be in the other list (which hasn't emitted yet). This avoids a
        // false pop when archivedModules fires before activeModules has a value (or vice-versa).
        androidx.lifecycle.Observer<List<Module>> moduleObserver = ignored -> {
            List<Module> active   = moduleViewModel.activeModules.getValue();
            List<Module> archived = moduleViewModel.archivedModules.getValue();
            if (active == null || archived == null) return; // wait for both to deliver
            Module found = findModule(active, moduleId);
            if (found == null) found = findModule(archived, moduleId);
            if (found == null) {
                Navigation.findNavController(requireView()).popBackStack();
                return;
            }
            populateHeader(found);
        };
        moduleViewModel.activeModules.observe(getViewLifecycleOwner(), moduleObserver);
        moduleViewModel.archivedModules.observe(getViewLifecycleOwner(), moduleObserver);

        // Assessments list — keep fullList in sync, then re-apply the current search filter
        assessmentViewModel.assessments.observe(getViewLifecycleOwner(), list -> {
            fullList.clear();
            if (list != null) fullList.addAll(list);
            applyFilter();
        });

        // Search field
        binding.etSearchAssessment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // FAB — add new assessment
        binding.fabAddAssessment.setOnClickListener(v -> {
            assessmentViewModel.setEditingAssessment(null);
            new AssessmentBottomSheetFragment().show(getChildFragmentManager(), "add_assessment");
        });

        // Edit module button
        binding.btnEditModule.setOnClickListener(v -> {
            Module m = getCurrentModule();
            if (m != null) moduleViewModel.setEditingModule(m);
            new ModuleBottomSheetFragment().show(getChildFragmentManager(), "edit_module");
        });
    }

    private void applyFilter() {
        if (binding == null) return;
        if (searchQuery.isEmpty()) {
            adapter.submitList(new ArrayList<>(fullList));
            return;
        }
        List<Assessment> filtered = new ArrayList<>();
        for (Assessment a : fullList) {
            if (a.getTitle() != null && a.getTitle().toLowerCase().contains(searchQuery)) {
                filtered.add(a);
            }
        }
        adapter.submitList(filtered);
    }

    private void populateHeader(Module m) {
        binding.tvModuleName.setText(m.getName());

        StringBuilder meta = new StringBuilder();
        if (m.getModuleCode() != null && !m.getModuleCode().isEmpty()) meta.append(m.getModuleCode());
        if (m.getSemester() != null && !m.getSemester().isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(m.getSemester());
        }
        binding.tvModuleMeta.setText(meta.toString());
        binding.tvModuleMeta.setVisibility(meta.length() > 0 ? View.VISIBLE : View.GONE);

        try {
            binding.viewColorStrip.setBackgroundColor(
                    m.getColor() != null ? Color.parseColor(m.getColor()) : 0xFF9E9E9E);
        } catch (IllegalArgumentException e) {
            binding.viewColorStrip.setBackgroundColor(0xFF9E9E9E);
        }
    }

    private Module findModule(List<Module> list, String id) {
        if (list == null) return null;
        for (Module m : list) if (m.getModuleId().equals(id)) return m;
        return null;
    }

    private Module getCurrentModule() {
        List<Module> active = moduleViewModel.activeModules.getValue();
        Module m = findModule(active, moduleId);
        if (m != null) return m;
        return findModule(moduleViewModel.archivedModules.getValue(), moduleId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
