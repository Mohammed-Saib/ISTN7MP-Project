package com.example.mpproject.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.navigation.Navigation;

import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.CalendarEventRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.example.mpproject.databinding.FragmentCalendarBinding;
import com.example.mpproject.presentation.adapter.AgendaAdapter;
import com.example.mpproject.presentation.adapter.CalendarGridAdapter;
import com.example.mpproject.presentation.view.calendar.EventBottomSheetFragment;
import com.example.mpproject.presentation.viewmodel.CalendarViewModel;
import com.example.mpproject.presentation.viewmodel.CalendarViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.mpproject.domain.model.Module;

// [View] Calendar screen — month grid + day agenda. All state lives in CalendarViewModel.
public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private CalendarGridAdapter gridAdapter;
    private AgendaAdapter agendaAdapter;

    // Maps moduleId → module name for resolving module chips in the agenda
    private final Map<String, String> moduleNameMap = new HashMap<>();

    private static final SimpleDateFormat MONTH_FMT =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat AGENDA_DATE_FMT =
            new SimpleDateFormat("EEE, d MMM", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return; // auth guard — MainActivity redirects if not signed in

        // Build repositories from the shared AppDatabase singleton
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        CalendarViewModelFactory factory = new CalendarViewModelFactory(
                new CalendarEventRepositoryImpl(db.calendarEventDao()),
                new TodoRepositoryImpl(db.todoDao()),
                new ModuleRepositoryImpl(db.moduleDao()),
                user.getUid());

        viewModel = new ViewModelProvider(this, factory).get(CalendarViewModel.class);

        setupGrid();
        setupAgenda();
        observeViewModel();
        setupControls();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupGrid() {
        gridAdapter = new CalendarGridAdapter(dayOfMonth -> {
            // When user taps a day, update selectedDay in the VM
            Calendar month = viewModel.getDisplayedMonth().getValue();
            if (month == null) return;
            Calendar tapped = (Calendar) month.clone();
            tapped.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            viewModel.selectDay(tapped);
        });

        // 7 columns — each cell is 1/7 of the grid width
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        binding.rvCalendarGrid.setAdapter(gridAdapter);
        // Disable scroll so the NestedScrollView handles all vertical movement
        binding.rvCalendarGrid.setNestedScrollingEnabled(false);
    }

    private void setupAgenda() {
        agendaAdapter = new AgendaAdapter(
                event -> {
                    // Tap on an event row → open bottom sheet in edit mode
                    viewModel.setEditingEvent(event);
                    new EventBottomSheetFragment()
                            .show(getChildFragmentManager(), "edit_event");
                },
                (todo, checked) -> viewModel.toggleTodo(todo, checked)
        );
        binding.rvDayAgenda.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDayAgenda.setAdapter(agendaAdapter);
        binding.rvDayAgenda.setNestedScrollingEnabled(false);
    }

    // ── Observers ────────────────────────────────────────────────────────────

    private void observeViewModel() {
        // Grid cells — update when month or selection changes
        viewModel.gridItems.observe(getViewLifecycleOwner(),
                cells -> gridAdapter.submitList(cells));

        // Agenda items — update when selected day changes
        viewModel.selectedDayItems.observe(getViewLifecycleOwner(), items -> {
            agendaAdapter.submitList(items);
            binding.tvNoEvents.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            // After agenda updates, resolve module names for visible event rows
            resolveModuleNames();
        });

        // Month label
        viewModel.getDisplayedMonth().observe(getViewLifecycleOwner(), month ->
                binding.tvMonthYear.setText(MONTH_FMT.format(month.getTime())));

        // Agenda header ("Today, 17 May" or "Mon, 18 May")
        viewModel.getSelectedDay().observe(getViewLifecycleOwner(), day -> {
            Calendar today = Calendar.getInstance();
            boolean isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            String label = isToday
                    ? "Today, " + new SimpleDateFormat("d MMM", Locale.getDefault()).format(day.getTime())
                    : AGENDA_DATE_FMT.format(day.getTime());
            binding.tvSelectedDate.setText(label);
        });

        // Build moduleId → name map for agenda chips
        viewModel.modules.observe(getViewLifecycleOwner(), modules -> {
            moduleNameMap.clear();
            if (modules != null) {
                for (Module m : modules) moduleNameMap.put(m.getModuleId(), m.getName());
            }
            resolveModuleNames();
        });
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    private void setupControls() {
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_calendarFragment_to_settingsFragment));

        binding.btnPrevMonth.setOnClickListener(v -> viewModel.navigateMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> viewModel.navigateMonth(1));

        // FAB opens the bottom sheet with no pre-filled event (add mode)
        binding.fabAddEvent.setOnClickListener(v -> {
            viewModel.setEditingEvent(null);
            new EventBottomSheetFragment()
                    .show(getChildFragmentManager(), "add_event");
        });
    }

    // Walks visible agenda event rows and fills module name chips from the local map
    private void resolveModuleNames() {
        int count = binding.rvDayAgenda.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = binding.rvDayAgenda.getChildAt(i);
            View tvModule = child.findViewById(com.example.mpproject.R.id.tv_event_module);
            if (tvModule instanceof android.widget.TextView) {
                Object tag = tvModule.getTag();
                if (tag instanceof String) {
                    String name = moduleNameMap.get((String) tag);
                    if (name != null) {
                        ((android.widget.TextView) tvModule).setText(name);
                        tvModule.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
