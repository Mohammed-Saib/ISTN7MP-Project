package com.example.mpproject.presentation.view;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.PomodoroPreferences;
import com.example.mpproject.data.local.dao.TodoDao;
import com.example.mpproject.data.local.entity.TodoEntity;
import com.example.mpproject.data.repository.PomodoroRepositoryImpl;
import com.example.mpproject.presentation.adapter.MyTaskAdapter;
import com.example.mpproject.presentation.adapter.TodoPanelAdapter;
import com.example.mpproject.presentation.model.PomodoroUiState;
import com.example.mpproject.presentation.viewmodel.PomodoroViewModel;
import com.example.mpproject.presentation.viewmodel.PomodoroViewModelFactory;
import com.example.mpproject.presentation.viewmodel.SessionViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class TimerFragment extends Fragment {

    private PomodoroViewModel viewModel;
    private TextView txtTimer, tabFocus, tabShortBreak, tabLongBreak, txtActiveTask;
    private ImageView imgBackground;
    private ImageButton btnPlay, btnPause;
    private BottomSheetDialog taskPanel;
    private TodoDao todoDao;
    private String currentUserId;

    private SessionViewModel sessionViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.timer_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── ViewModel setup ───────────────────────────────────────────────────
        PomodoroPreferences prefs = new PomodoroPreferences(requireContext());
        PomodoroRepositoryImpl repo = new PomodoroRepositoryImpl(prefs);
        PomodoroViewModelFactory factory = new PomodoroViewModelFactory(repo);
        viewModel = new ViewModelProvider(requireActivity(), factory)
                .get(PomodoroViewModel.class);

        // ── Shared session ViewModel (populated by TasksFragment) ─────────────
        sessionViewModel = new ViewModelProvider(requireActivity())
                .get(SessionViewModel.class);

        // ── Bind views ────────────────────────────────────────────────────────
        txtTimer      = view.findViewById(R.id.txtTimer);
        imgBackground = view.findViewById(R.id.imgBackground);
        tabFocus      = view.findViewById(R.id.tabFocus);
        tabShortBreak = view.findViewById(R.id.tabShortBreak);
        tabLongBreak  = view.findViewById(R.id.tabLongBreak);
        btnPlay       = view.findViewById(R.id.btnPlay);
        btnPause      = view.findViewById(R.id.btnPause);
        txtActiveTask = view.findViewById(R.id.txtActiveTask);

        // ── Observe UI state ──────────────────────────────────────────────────
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);

        // ── Button clicks ─────────────────────────────────────────────────────
        btnPlay.setOnClickListener(v       -> viewModel.onPlayClicked());
        btnPause.setOnClickListener(v      -> viewModel.onPauseClicked());
        tabFocus.setOnClickListener(v      -> viewModel.setMode(PomodoroViewModel.MODE_FOCUS));
        tabShortBreak.setOnClickListener(v -> viewModel.setMode(PomodoroViewModel.MODE_SHORT_BREAK));
        tabLongBreak.setOnClickListener(v  -> viewModel.setMode(PomodoroViewModel.MODE_LONG_BREAK));
        view.findViewById(R.id.btnSettings).setOnClickListener(v -> openSettings());
        view.findViewById(R.id.btnTodo).setOnClickListener(v -> openTaskPanel());

        // ── Firebase user ─────────────────────────────────────────────────────
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUserId = user.getUid();

        // ── Room DAO ──────────────────────────────────────────────────────────
        todoDao = AppDatabase.getDatabase(requireContext()).todoDao();

        // ── Load recommended tasks for the timer's own task panel ────────────
        loadRecommendedTasks();

        // ── Load session tasks from TasksFragment (if a session was started) ─
        sessionViewModel.getSessionTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                // Push into PomodoroViewModel's myTasks list
                viewModel.setMyTasksFromSession(tasks);
                // Auto-set the first task as the active focus label
                // but only if nothing is already active
                PomodoroUiState current = viewModel.getUiState().getValue();
                if (current != null && current.activeTaskLabel == null) {
                    viewModel.setActiveTask(tasks.get(0));
                }
            }
        });
    }
    private void loadRecommendedTasks() {
        if (currentUserId == null) return;

        long now  = System.currentTimeMillis();
        long week = now + (7L * 24 * 60 * 60 * 1000);

        todoDao.getAllByUser(currentUserId).observe(getViewLifecycleOwner(), all -> {
            if (all == null) return;

            List<TodoEntity> recommended = new ArrayList<>();
            for (TodoEntity t : all) {
                boolean dueSoon      = t.getDueDate() != null
                        && t.getDueDate() >= now
                        && t.getDueDate() <= week;
                boolean highPriority = "HIGH".equals(t.getPriority());
                if (dueSoon || highPriority) {
                    recommended.add(t);
                }
            }
            viewModel.setRecommendedTasks(recommended);
        });
    }

    // ── Render state from ViewModel ───────────────────────────────────────────
    private void renderState(PomodoroUiState state) {
        txtTimer.setText(state.TimeDisplay);

        btnPlay.setVisibility(state.isRunning  ? View.GONE    : View.VISIBLE);
        btnPause.setVisibility(state.isRunning ? View.VISIBLE : View.GONE);

        updateTabs(state.currentMode);
        updateBackground(state.currentMode, state.theme);

        // Show active task label above tabs
        if (state.activeTaskLabel != null && !state.activeTaskLabel.isEmpty()) {
            txtActiveTask.setText(state.activeTaskLabel);
            txtActiveTask.setVisibility(View.VISIBLE);
        } else {
            txtActiveTask.setVisibility(View.GONE);
        }
    }

    // ── Tab highlight logic ───────────────────────────────────────────────────
    private void updateTabs(int mode) {
        tabFocus.setBackgroundResource(android.R.color.transparent);
        tabShortBreak.setBackgroundResource(android.R.color.transparent);
        tabLongBreak.setBackgroundResource(android.R.color.transparent);
        tabFocus.setTextColor(0xCCFFFFFF);
        tabShortBreak.setTextColor(0xCCFFFFFF);
        tabLongBreak.setTextColor(0xCCFFFFFF);

        TextView active = mode == PomodoroViewModel.MODE_SHORT_BREAK ? tabShortBreak
                : mode == PomodoroViewModel.MODE_LONG_BREAK          ? tabLongBreak
                : tabFocus;
        active.setBackgroundResource(R.drawable.tab_active_bg);
        active.setTextColor(0xFFFFFFFF);
    }

    // ── Background image — Cat auto-follows system dark/light mode ────────────
    private void updateBackground(int mode, String theme) {
        int resId;

        if ("Snoopy".equals(theme)) {
            resId = mode == PomodoroViewModel.MODE_SHORT_BREAK ? R.drawable.snoopy_sb
                    : mode == PomodoroViewModel.MODE_LONG_BREAK  ? R.drawable.snoopy_lb
                    : R.drawable.snoopy_focus;

        } else {
            // Cat theme — pick dark or light variant based on system setting
            boolean isDark = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

            if (isDark) {
                resId = mode == PomodoroViewModel.MODE_SHORT_BREAK ? R.drawable.dmlogo_sb
                        : mode == PomodoroViewModel.MODE_LONG_BREAK  ? R.drawable.dmlogo_lb
                        : R.drawable.dmlogo_focus;
            } else {
                resId = mode == PomodoroViewModel.MODE_SHORT_BREAK ? R.drawable.lmlogo_sb
                        : mode == PomodoroViewModel.MODE_LONG_BREAK  ? R.drawable.lmlogo_lb
                        : R.drawable.lmlogo_focus;
            }
        }

        imgBackground.setImageResource(resId);
    }

    // ── Open timer settings dialog ────────────────────────────────────────────
    private void openSettings() {
        TimerSettingFragment dialog = TimerSettingFragment.newInstance(viewModel.getSettings());
        dialog.setOnSaveListener(newSettings -> viewModel.saveSettings(newSettings));
        dialog.show(getParentFragmentManager(), "settings");
    }

    // ── Open task panel bottom sheet ──────────────────────────────────────────
    private void openTaskPanel() {
        taskPanel = new BottomSheetDialog(requireContext());
        View panelView = LayoutInflater.from(requireContext())
                .inflate(R.layout.timer_task_panel, null);
        taskPanel.setContentView(panelView);

        View panelRecommended = panelView.findViewById(R.id.panelRecommended);
        View panelMyTasks     = panelView.findViewById(R.id.panelMyTasks);

        // ── Tab switching ─────────────────────────────────────────────────────
        TabLayout tabs = panelView.findViewById(R.id.tabLayoutTasks);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean isRecommended = tab.getPosition() == 0;
                panelRecommended.setVisibility(isRecommended ? View.VISIBLE : View.GONE);
                panelMyTasks.setVisibility(isRecommended     ? View.GONE    : View.VISIBLE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // ── Recommended tab ───────────────────────────────────────────────────
        RecyclerView rvRecommended = panelView.findViewById(R.id.rvRecommendedTasks);
        rvRecommended.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<TodoEntity> currentRec = viewModel.getRecommendedTasks().getValue();
        TodoPanelAdapter recAdapter = new TodoPanelAdapter(
                currentRec != null ? currentRec : new ArrayList<>(),
                todo -> {
                    // Tap a recommended task → set as active focus label
                    viewModel.setActiveTask(todo.getTitle());
                    updatePanelFooter(panelView);
                });
        rvRecommended.setAdapter(recAdapter);

        viewModel.getRecommendedTasks().observe(getViewLifecycleOwner(),
                todos -> recAdapter.updateList(todos != null ? todos : new ArrayList<>()));

        // Accept All → copies every recommended task into My Tasks, switches tab
        panelView.findViewById(R.id.btnAcceptAll).setOnClickListener(v -> {
            viewModel.acceptAllRecommended();
            TabLayout.Tab myTasksTab = tabs.getTabAt(1);
            if (myTasksTab != null) myTasksTab.select();
        });

        // ── My Tasks tab ──────────────────────────────────────────────────────
        RecyclerView rvMyTasks = panelView.findViewById(R.id.rvMyTasks);
        rvMyTasks.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<String> currentMy = viewModel.getMyTasks().getValue();
        MyTaskAdapter myAdapter = new MyTaskAdapter(
                currentMy != null ? currentMy : new ArrayList<>(),
                index -> viewModel.removeMyTask(index),   // long-press to remove
                label -> {
                    // Tap a My Task → set as active focus label
                    viewModel.setActiveTask(label);
                    updatePanelFooter(panelView);
                });
        rvMyTasks.setAdapter(myAdapter);

        viewModel.getMyTasks().observe(getViewLifecycleOwner(),
                tasks -> myAdapter.updateList(tasks != null ? tasks : new ArrayList<>()));

        // Manual add
        TextInputEditText etNewTask = panelView.findViewById(R.id.etNewTask);
        panelView.findViewById(R.id.btnAddTask).setOnClickListener(v -> {
            String text = etNewTask.getText() != null
                    ? etNewTask.getText().toString().trim() : "";
            if (!text.isEmpty()) {
                viewModel.addMyTask(text);
                etNewTask.setText("");
            }
        });

        // ── Footer: active task display + clear ───────────────────────────────
        updatePanelFooter(panelView);
        panelView.findViewById(R.id.btnClearTask).setOnClickListener(v -> {
            viewModel.clearActiveTask();
            updatePanelFooter(panelView);
        });

        taskPanel.show();
    }

    // Updates the "Focusing on: X" label at the bottom of the panel
    private void updatePanelFooter(View panelView) {
        TextView tv = panelView.findViewById(R.id.tvCurrentTask);
        PomodoroUiState state = viewModel.getUiState().getValue();
        String label = (state != null && state.activeTaskLabel != null
                && !state.activeTaskLabel.isEmpty())
                ? "Focusing on: " + state.activeTaskLabel
                : "No task selected";
        tv.setText(label);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (taskPanel != null && taskPanel.isShowing()) taskPanel.dismiss();
    }
}