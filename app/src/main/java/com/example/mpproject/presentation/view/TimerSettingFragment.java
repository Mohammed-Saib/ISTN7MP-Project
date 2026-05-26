package com.example.mpproject.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mpproject.R;
import com.example.mpproject.domain.model.PomodoroSettings;

public class TimerSettingFragment extends DialogFragment {

    // Interface so TimerFragment can receive the saved settings
    public interface OnSaveListener {
        void onSave(PomodoroSettings settings);
    }

    private OnSaveListener onSaveListener;
    private PomodoroSettings currentSettings;

    // Pass in current settings when opening the dialog
    public static TimerSettingFragment newInstance(PomodoroSettings settings) {
        TimerSettingFragment fragment = new TimerSettingFragment();
        Bundle args = new Bundle();
        args.putInt("focus",    settings.focusMin);
        args.putInt("short",    settings.shortBreakMin);
        args.putInt("long",     settings.longBreakMin);
        args.putInt("sessions", settings.sessionsBeforeLB);
        args.putString("theme", settings.theme);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnSaveListener(OnSaveListener listener) {
        this.onSaveListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSettings = new PomodoroSettings(
                    getArguments().getInt("focus",    25),
                    getArguments().getInt("short",    5),
                    getArguments().getInt("long",     15),
                    getArguments().getInt("sessions", 4),
                    getArguments().getString("theme", "Cat")
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.timer_settings_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Bind seekbars ─────────────────────────────────────────────────────
        SeekBar seekFocus    = view.findViewById(R.id.seekFocus);
        SeekBar seekShort    = view.findViewById(R.id.seekShort);
        SeekBar seekLong     = view.findViewById(R.id.seekLong);
        SeekBar seekSessions = view.findViewById(R.id.seekSessions);

        // ── Bind labels ───────────────────────────────────────────────────────
        TextView lblFocus    = view.findViewById(R.id.lblFocus);
        TextView lblShort    = view.findViewById(R.id.lblShort);
        TextView lblLong     = view.findViewById(R.id.lblLong);
        TextView lblSessions = view.findViewById(R.id.lblSessions);

        // ── Bind theme radio group and save button ────────────────────────────
        RadioGroup radioTheme = view.findViewById(R.id.radioTheme);
        Button btnSave        = view.findViewById(R.id.btnSaveSettings);

        // ── Populate seekbars with current values ─────────────────────────────
        seekFocus.setProgress(currentSettings.focusMin);
        seekShort.setProgress(currentSettings.shortBreakMin);
        seekLong.setProgress(currentSettings.longBreakMin);
        seekSessions.setProgress(currentSettings.sessionsBeforeLB);

        lblFocus.setText(currentSettings.focusMin + " min");
        lblShort.setText(currentSettings.shortBreakMin + " min");
        lblLong.setText(currentSettings.longBreakMin + " min");
        lblSessions.setText(currentSettings.sessionsBeforeLB + " sessions");

        // ── Pre-select the correct radio button ───────────────────────────────
        // Themes: "Cat" (auto dark/light), "Snoopy", "NewTheme"
        switch (currentSettings.theme) {
            case "Snoopy":    radioTheme.check(R.id.radioSnoopy);   break;
            case "NewTheme":  radioTheme.check(R.id.radioStudy); break;
            default:          radioTheme.check(R.id.radioCat);      break; // Cat is default
        }

        // ── Live label updates as seekbars move ───────────────────────────────
        seekFocus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                lblFocus.setText(Math.max(p, 1) + " min");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s)  {}
        });

        seekShort.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                lblShort.setText(Math.max(p, 1) + " min");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s)  {}
        });

        seekLong.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                lblLong.setText(Math.max(p, 1) + " min");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s)  {}
        });

        seekSessions.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                lblSessions.setText(Math.max(p, 1) + " sessions");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s)  {}
        });

        // ── Save button ───────────────────────────────────────────────────────
        btnSave.setOnClickListener(v -> {
            int focusVal   = Math.max(seekFocus.getProgress(),    1);
            int shortVal   = Math.max(seekShort.getProgress(),    1);
            int longVal    = Math.max(seekLong.getProgress(),     1);
            int sessionVal = Math.max(seekSessions.getProgress(), 1);

            // Default is Cat (auto dark/light mode)
            String theme = "Cat";
            int checkedId = radioTheme.getCheckedRadioButtonId();
            if      (checkedId == R.id.radioSnoopy)   theme = "Snoopy";
            else if (checkedId == R.id.radioStudy)  theme = "Study Space";

            PomodoroSettings newSettings = new PomodoroSettings(
                    focusVal, shortVal, longVal, sessionVal, theme);

            if (onSaveListener != null) {
                onSaveListener.onSave(newSettings);
            }

            dismiss();
        });
    }

    // Make the dialog full width
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}