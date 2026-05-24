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

import android.text.Editable;
import android.text.TextWatcher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.mpproject.domain.model.Module;
import com.example.mpproject.presentation.view.calendar.AgendaItem;

// [View] Calendar screen — month grid + day agenda. All state lives in CalendarViewModel.
public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private CalendarGridAdapter gridAdapter;
    private AgendaAdapter agendaAdapter;

    // true = list (upcoming) mode; false = calendar grid + day agenda mode (default)
    private boolean isListMode = false;

    private final List<AgendaItem> fullUpcomingList = new ArrayList<>();
    private String searchQuery = "";

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

        // Agenda items — update when selected day changes (ignored in list mode)
        viewModel.selectedDayItems.observe(getViewLifecycleOwner(), items -> {
            if (isListMode) return;
            agendaAdapter.submitList(items);
            binding.tvNoEvents.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            binding.tvNoEvents.setText("Nothing scheduled");
        });

        // Upcoming items — used by the list view (ignored in calendar mode)
        viewModel.upcomingItems.observe(getViewLifecycleOwner(), items -> {
            fullUpcomingList.clear();
            if (items != null) fullUpcomingList.addAll(items);
            if (!isListMode) return;
            applyListFilter();
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

        // Pass moduleId → name map to adapter so chips resolve correctly at bind time
        viewModel.modules.observe(getViewLifecycleOwner(), modules -> {
            Map<String, String> map = new HashMap<>();
            if (modules != null) {
                for (Module m : modules) map.put(m.getModuleId(), m.getName());
            }
            agendaAdapter.setModuleNameMap(map);
        });
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    private void setupControls() {
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_calendarFragment_to_settingsFragment));

        binding.btnPrevMonth.setOnClickListener(v -> viewModel.navigateMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> viewModel.navigateMonth(1));

        binding.btnToggleView.setOnClickListener(v -> {
            isListMode = !isListMode;
            applyViewMode();
        });

        binding.etSearchList.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                if (isListMode) applyListFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // FAB opens the bottom sheet with no pre-filled event (add mode)
        binding.fabAddEvent.setOnClickListener(v -> {
            viewModel.setEditingEvent(null);
            new EventBottomSheetFragment()
                    .show(getChildFragmentManager(), "add_event");
        });
    }

    // Switches between calendar-grid mode and upcoming-list mode
    private void applyViewMode() {
        // Tell adapter whether to show the date on event cards (needed in list mode, redundant in day mode)
        agendaAdapter.setShowDate(isListMode);

        if (isListMode) {
            binding.llCalendarSection.setVisibility(View.GONE);
            binding.llMonthNav.setVisibility(View.GONE);
            binding.tvSelectedDate.setText("Upcoming");
            binding.tilSearchList.setVisibility(View.VISIBLE);
            // Icon changes to calendar so the user knows tapping returns to grid view
            binding.btnToggleView.setImageResource(R.drawable.ic_nav_calendar);
            applyListFilter();
        } else {
            binding.llCalendarSection.setVisibility(View.VISIBLE);
            binding.llMonthNav.setVisibility(View.VISIBLE);
            binding.tilSearchList.setVisibility(View.GONE);
            binding.etSearchList.setText("");
            searchQuery = "";
            // Icon changes to list so the user knows tapping switches to list view
            binding.btnToggleView.setImageResource(R.drawable.ic_view_list);
            // Restore the selected-day header label
            Calendar day = viewModel.getSelectedDay().getValue();
            if (day != null) {
                Calendar today = Calendar.getInstance();
                boolean isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                        && day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
                binding.tvSelectedDate.setText(isToday
                        ? "Today, " + new java.text.SimpleDateFormat("d MMM", Locale.getDefault()).format(day.getTime())
                        : AGENDA_DATE_FMT.format(day.getTime()));
            }
            List<AgendaItem> items = viewModel.selectedDayItems.getValue();
            if (items != null) {
                agendaAdapter.submitList(items);
                binding.tvNoEvents.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                binding.tvNoEvents.setText("Nothing scheduled");
            }
        }
    }

    private void applyListFilter() {
        if (binding == null) return;
        List<AgendaItem> result;
        if (searchQuery.isEmpty()) {
            result = new ArrayList<>(fullUpcomingList);
        } else {
            result = new ArrayList<>();
            for (AgendaItem item : fullUpcomingList) {
                String title = item.getType() == AgendaItem.TYPE_EVENT
                        ? item.getEvent().getTitle()
                        : item.getTodo().getTitle();
                if (title != null && title.toLowerCase().contains(searchQuery)) {
                    result.add(item);
                }
            }
        }
        agendaAdapter.submitList(result);
        binding.tvNoEvents.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvNoEvents.setText(searchQuery.isEmpty() ? "Nothing upcoming" : "No results");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
