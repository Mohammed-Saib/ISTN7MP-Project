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
import com.example.mpproject.presentation.viewmodel.SessionViewModel;
import com.example.mpproject.presentation.viewmodel.TaskViewModel;
import com.example.mpproject.presentation.viewmodel.TaskViewModelFactory;
import com.example.mpproject.presentation.adapter.SessionTaskAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;

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

    private SessionViewModel sessionViewModel;

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

        // Session ViewModel — scoped to activity so TimerFragment shares it
        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        // "Start a Session" button — add this button to fragment_tasks.xml (see below)
        binding.btnStartSession.setOnClickListener(v -> openSessionPanel());

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

    private void openSessionPanel() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View panel = LayoutInflater.from(requireContext())
                .inflate(R.layout.session_panel, null);
        sheet.setContentView(panel);

        View panelRec    = panel.findViewById(R.id.panelRecommended);
        View panelMyList = panel.findViewById(R.id.panelMyList);

        // ── Tab switching ──────────────────────────────────────────────
        TabLayout tabs = panel.findViewById(R.id.sessionTabLayout);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                boolean rec = tab.getPosition() == 0;
                panelRec.setVisibility(rec    ? View.VISIBLE : View.GONE);
                panelMyList.setVisibility(rec ? View.GONE    : View.VISIBLE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // ── Recommended list ───────────────────────────────────────────
        RecyclerView rvRec = panel.findViewById(R.id.rvRecommended);
        rvRec.setLayoutManager(new LinearLayoutManager(requireContext()));
        SessionTaskAdapter recAdapter = new SessionTaskAdapter(true);
        rvRec.setAdapter(recAdapter);

        // Build recommended list: HIGH priority + due within 7 days, sorted
        taskViewModel.allTodos.observe(getViewLifecycleOwner(), all -> {
            if (all == null) return;
            long now  = System.currentTimeMillis();
            long week = now + 7L * 24 * 60 * 60 * 1000;
            List<Todo> recommended = new ArrayList<>();
            for (Todo t : all) {
                if (t.isCompleted()) continue;
                boolean dueSoon     = t.getDueDate() != null && t.getDueDate() >= now && t.getDueDate() <= week;
                boolean highPriority = "HIGH".equals(t.getPriority());
                if (dueSoon || highPriority) recommended.add(t);
            }
            // Sort: HIGH first, then by due date
            recommended.sort((a, b) -> {
                int pa = priorityScore(a.getPriority()), pb = priorityScore(b.getPriority());
                if (pa != pb) return pb - pa;
                Long da = a.getDueDate(), db = b.getDueDate();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return Long.compare(da, db);
            });
            recAdapter.updateTodos(recommended);
        });

        // Tap a recommended task → add to session list
        recAdapter.setTapListener(pos -> {
            sessionViewModel.addTask(recAdapter.getTodoAt(pos).getTitle());
            // Switch to My Session List tab to confirm
            TabLayout.Tab myTab = tabs.getTabAt(1);
            if (myTab != null) myTab.select();
        });

        // Accept All
        panel.findViewById(R.id.btnAcceptAll).setOnClickListener(v -> {
            for (int i = 0; i < recAdapter.getItemCount(); i++) {
                sessionViewModel.addTask(recAdapter.getTodoAt(i).getTitle());
            }
            TabLayout.Tab myTab = tabs.getTabAt(1);
            if (myTab != null) myTab.select();
        });

        // ── My Session List ────────────────────────────────────────────
        RecyclerView rvList = panel.findViewById(R.id.rvSessionList);
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        SessionTaskAdapter listAdapter = new SessionTaskAdapter(false);
        rvList.setAdapter(listAdapter);

        sessionViewModel.getSessionTasks().observe(getViewLifecycleOwner(),
                tasks -> listAdapter.updateLabels(tasks));

        // Manual add
        TextInputEditText etTask = panel.findViewById(R.id.etSessionTask);
        panel.findViewById(R.id.btnAddSessionTask).setOnClickListener(v -> {
            String text = etTask.getText() != null ? etTask.getText().toString().trim() : "";
            if (!text.isEmpty()) {
                sessionViewModel.addTask(text);
                etTask.setText("");
            }
        });

        // Long-press to remove from session list
        listAdapter.setLongListener(pos -> sessionViewModel.removeTask(pos));

        // ── Start Session → navigate to timer ─────────────────────────
        panel.findViewById(R.id.btnStartSession).setOnClickListener(v -> {
            if (sessionViewModel.getSessionTasks().getValue() == null
                    || sessionViewModel.getSessionTasks().getValue().isEmpty()) {
                Toast.makeText(requireContext(),
                        "Add at least one task to start a session", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_tasksFragment_to_timerFragment);
        });

        sheet.show();
    }

    private int priorityScore(String p) {
        if ("HIGH".equals(p))   return 3;
        if ("MEDIUM".equals(p)) return 2;
        if ("LOW".equals(p))    return 1;
        return 0;
    }
}
