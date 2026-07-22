package com.example.mpproject.presentation.view;

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
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.databinding.FragmentModulesBinding;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.presentation.adapter.ModuleAdapter;
import com.example.mpproject.presentation.view.modules.ModuleBottomSheetFragment;
import com.example.mpproject.presentation.viewmodel.ModuleViewModel;
import com.example.mpproject.presentation.viewmodel.ModuleViewModelFactory;
import com.example.mpproject.data.local.LocalSessionManager;

import java.util.ArrayList;
import java.util.List;

// [View] Modules screen — active + archived list, inline search, add/edit via ModuleBottomSheetFragment.
public class ModulesFragment extends Fragment {

    private FragmentModulesBinding binding;
    private ModuleViewModel viewModel;
    private ModuleAdapter adapter;

    private final List<Module> fullActive = new ArrayList<>();
    private final List<Module> fullArchived = new ArrayList<>();
    private String searchQuery = "";
    private boolean showArchived = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentModulesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userId = LocalSessionManager.getCurrentUserId(requireContext());
        if (userId == null) return;

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        ModuleViewModelFactory factory = new ModuleViewModelFactory(
                new ModuleRepositoryImpl(db.moduleDao()), userId);
        viewModel = new ViewModelProvider(this, factory).get(ModuleViewModel.class);

        adapter = new ModuleAdapter(module -> {
            Bundle args = new Bundle();
            args.putString("moduleId", module.getModuleId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_modulesFragment_to_moduleDetailFragment, args);
        });

        binding.rvModules.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvModules.setAdapter(adapter);

        // Open Research Organiser
        binding.cardResearchOrganiser.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.researchFragment)
        );

        // Archive filter chips
        binding.chipActive.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) {
                showArchived = false;
                updateList();
            }
        });

        binding.chipArchived.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) {
                showArchived = true;
                updateList();
            }
        });

        viewModel.activeModules.observe(getViewLifecycleOwner(), active -> {
            fullActive.clear();
            if (active != null) fullActive.addAll(active);
            updateList();
        });

        viewModel.archivedModules.observe(getViewLifecycleOwner(), archived -> {
            fullArchived.clear();
            if (archived != null) fullArchived.addAll(archived);
            updateList();
        });

        binding.fabAddModule.setOnClickListener(v -> {
            viewModel.setEditingModule(null);
            new ModuleBottomSheetFragment().show(getChildFragmentManager(), "add_module");
        });

        // Toggle inline search row on search icon tap
        binding.btnSearchModule.setOnClickListener(v -> {
            boolean isShown = binding.searchRow.getVisibility() == View.VISIBLE;
            binding.searchRow.setVisibility(isShown ? View.GONE : View.VISIBLE);

            if (!isShown) {
                binding.etSearchModule.requestFocus();
            } else {
                binding.etSearchModule.setText("");
                searchQuery = "";
                updateList();
            }
        });

        binding.etSearchModule.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                updateList();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_modulesFragment_to_settingsFragment));
    }

    private void updateList() {
        List<Module> source = showArchived
                ? new ArrayList<>(fullArchived)
                : new ArrayList<>(fullActive);

        if (searchQuery.isEmpty()) {
            adapter.submitList(source);
            return;
        }

        List<Module> filtered = new ArrayList<>();

        for (Module m : source) {
            boolean nameMatch = m.getName() != null
                    && m.getName().toLowerCase().contains(searchQuery);

            boolean codeMatch = m.getModuleCode() != null
                    && m.getModuleCode().toLowerCase().contains(searchQuery);

            if (nameMatch || codeMatch) {
                filtered.add(m);
            }
        }

        adapter.submitList(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}