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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.example.mpproject.databinding.FragmentTasksBinding;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.presentation.adapter.TaskAdapter;
import com.example.mpproject.presentation.view.todo.TodoBottomSheetFragment;
import com.example.mpproject.presentation.viewmodel.TaskViewModel;
import com.example.mpproject.presentation.viewmodel.TaskViewModelFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [View] To-Do screen — search, filter chips, task list, add/edit via TodoBottomSheetFragment.
public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private FragmentTasksBinding binding;
    private TaskViewModel taskViewModel;
    private TaskAdapter taskAdapter;
    private LiveData<List<Todo>> currentSource;

    private final Map<String, String> moduleNameMap = new HashMap<>();
    private List<Todo> currentFullList = new ArrayList<>();
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskAdapter = new TaskAdapter(this);
        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.tasksRecyclerView.setAdapter(taskAdapter);

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        TaskViewModelFactory factory = new TaskViewModelFactory(
                new TodoRepositoryImpl(db.todoDao()),
                new ModuleRepositoryImpl(db.moduleDao()));
        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        observeList(taskViewModel.allTodos);

        taskViewModel.modules.observe(getViewLifecycleOwner(), modules -> {
            moduleNameMap.clear();
            if (modules != null)
                for (Module m : modules) moduleNameMap.put(m.getModuleId(), m.getName());
            taskAdapter.setModuleNameMap(moduleNameMap);
        });

        setupSearch();
        setupFilterChips();

        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_tasksFragment_to_settingsFragment));

        binding.fabAddTask.setOnClickListener(v -> {
            taskViewModel.setEditingTodo(null);
            new TodoBottomSheetFragment().show(getChildFragmentManager(), "add_task");
        });
    }

    private void setupSearch() {
        binding.btnSearchTask.setOnClickListener(v -> {
            boolean visible = binding.searchRow.getVisibility() == View.VISIBLE;
            binding.searchRow.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (!visible) {
                binding.etSearchTask.requestFocus();
            } else {
                binding.etSearchTask.setText("");
                searchQuery = "";
                applySearch();
            }
        });

        binding.etSearchTask.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s != null ? s.toString().trim() : "";
                applySearch();
            }
        });
    }

    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_completed) {
                observeList(taskViewModel.completedTodos);
            } else if (id == R.id.chip_high_priority) {
                observeList(taskViewModel.highPriorityTodos);
            } else if (id == R.id.chip_medium_priority) {
                observeList(taskViewModel.mediumPriorityTodos);
            } else if (id == R.id.chip_low_priority) {
                observeList(taskViewModel.lowPriorityTodos);
            } else {
                observeList(taskViewModel.allTodos);
            }
        });
    }

    private void observeList(LiveData<List<Todo>> source) {
        if (currentSource != null) currentSource.removeObservers(getViewLifecycleOwner());
        currentSource = source;
        currentSource.observe(getViewLifecycleOwner(), todos -> {
            currentFullList = todos != null ? todos : new ArrayList<>();
            applySearch();
        });
    }

    private void applySearch() {
        if (searchQuery.isEmpty()) {
            taskAdapter.submitList(new ArrayList<>(currentFullList));
            return;
        }
        String lower = searchQuery.toLowerCase();
        List<Todo> filtered = new ArrayList<>();
        for (Todo t : currentFullList) {
            if (t.getTitle().toLowerCase().contains(lower)) filtered.add(t);
        }
        taskAdapter.submitList(filtered);
    }

    @Override
    public void onTaskClick(Todo todo) {
        taskViewModel.setEditingTodo(todo);
        new TodoBottomSheetFragment().show(getChildFragmentManager(), "edit_task");
    }

    @Override
    public void onTaskCheckedChange(Todo todo, boolean isChecked) {
        taskViewModel.toggleTask(todo, isChecked);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
