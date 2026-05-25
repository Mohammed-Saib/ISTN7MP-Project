package com.example.mpproject.presentation.view;

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
import com.example.mpproject.R;
import com.example.mpproject.data.local.PomodoroPreferences;
import com.example.mpproject.data.repository.PomodoroRepositoryImpl;
import com.example.mpproject.presentation.model.PomodoroUiState;
import com.example.mpproject.presentation.viewmodel.PomodoroViewModel;
import com.example.mpproject.presentation.viewmodel.PomodoroViewModelFactory;

public class TimerFragment extends Fragment {

    private PomodoroViewModel viewModel;
    private TextView txtTimer, tabFocus, tabShortBreak, tabLongBreak;
    private ImageView imgBackground;
    private ImageButton btnPlay, btnPause;

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

        // Wire up dependencies
        PomodoroPreferences    prefs   = new PomodoroPreferences(requireContext());
        PomodoroRepositoryImpl repo    = new PomodoroRepositoryImpl(prefs);
        PomodoroViewModelFactory factory = new PomodoroViewModelFactory(repo);
        viewModel = new ViewModelProvider(requireActivity(), factory)
                .get(PomodoroViewModel.class);

        // Bind views
        txtTimer      = view.findViewById(R.id.txtTimer);
        imgBackground = view.findViewById(R.id.imgBackground);
        tabFocus      = view.findViewById(R.id.tabFocus);
        tabShortBreak = view.findViewById(R.id.tabShortBreak);
        tabLongBreak  = view.findViewById(R.id.tabLongBreak);
        btnPlay       = view.findViewById(R.id.btnPlay);
        btnPause      = view.findViewById(R.id.btnPause);

        // Observe state
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);

        // Clicks
        btnPlay.setOnClickListener(v       -> viewModel.onPlayClicked());
        btnPause.setOnClickListener(v      -> viewModel.onPauseClicked());
        tabFocus.setOnClickListener(v      -> viewModel.setMode(PomodoroViewModel.MODE_FOCUS));
        tabShortBreak.setOnClickListener(v -> viewModel.setMode(PomodoroViewModel.MODE_SHORT_BREAK));
        tabLongBreak.setOnClickListener(v  -> viewModel.setMode(PomodoroViewModel.MODE_LONG_BREAK));
        view.findViewById(R.id.btnSettings).setOnClickListener(v -> openSettings());
    }

    private void renderState(PomodoroUiState state) {
        txtTimer.setText(state.TimeDisplay);

        btnPlay.setVisibility(state.isRunning  ? View.GONE    : View.VISIBLE);
        btnPause.setVisibility(state.isRunning ? View.VISIBLE : View.GONE);

        updateTabs(state.currentMode);
        updateBackground(state.currentMode, state.theme);
    }

    private void updateTabs(int mode) {
        // Reset all to inactive
        tabFocus.setBackgroundResource(android.R.color.transparent);
        tabShortBreak.setBackgroundResource(android.R.color.transparent);
        tabLongBreak.setBackgroundResource(android.R.color.transparent);
        tabFocus.setTextColor(0xCCFFFFFF);
        tabShortBreak.setTextColor(0xCCFFFFFF);
        tabLongBreak.setTextColor(0xCCFFFFFF);

        // Highlight active tab
        TextView active = mode == PomodoroViewModel.MODE_SHORT_BREAK ? tabShortBreak
                : mode == PomodoroViewModel.MODE_LONG_BREAK  ? tabLongBreak
                : tabFocus;
        active.setBackgroundResource(R.drawable.tab_active_bg);
        active.setTextColor(0xFFFFFFFF);
    }

    private void updateBackground(int mode, String theme) {
        int resId;
        if (theme.equals("Snoopy")) {
            resId = mode == PomodoroViewModel.MODE_SHORT_BREAK ? R.drawable.snoopy_sb
                    : mode == PomodoroViewModel.MODE_LONG_BREAK  ? R.drawable.snoopy_lb
                    : R.drawable.snoopy_focus;
        } else if (theme.equals("Cat - Dark Mode")) {
            resId = mode == PomodoroViewModel.MODE_SHORT_BREAK ? R.drawable.dmlogo_sb
                    : mode == PomodoroViewModel.MODE_LONG_BREAK  ? R.drawable.dmlogo_lb
                    : R.drawable.dmlogo_focus;
        } else {
            resId = mode == PomodoroViewModel.MODE_SHORT_BREAK ? R.drawable.lmlogo_sb
                    : mode == PomodoroViewModel.MODE_LONG_BREAK  ? R.drawable.lmlogo_lb
                    : R.drawable.lmlogo_focus;
        }
        imgBackground.setImageResource(resId);
    }

    private void openSettings() {
        TimerSettingFragment dialog = TimerSettingFragment.newInstance(
                viewModel.getSettings());
        dialog.setOnSaveListener(newSettings -> viewModel.saveSettings(newSettings));
        dialog.show(getParentFragmentManager(), "settings");
    }
}