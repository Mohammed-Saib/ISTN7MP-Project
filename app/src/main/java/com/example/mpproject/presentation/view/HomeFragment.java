package com.example.mpproject.presentation.view;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mpproject.R;
import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.CalendarEventRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.example.mpproject.databinding.FragmentHomeBinding;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.presentation.view.calendar.AgendaItem;
import com.example.mpproject.presentation.viewmodel.CalendarViewModel;
import com.example.mpproject.presentation.viewmodel.CalendarViewModelFactory;
import com.example.mpproject.presentation.viewmodel.ModuleViewModel;
import com.example.mpproject.presentation.viewmodel.ModuleViewModelFactory;
import com.example.mpproject.presentation.viewmodel.TaskViewModel;
import com.example.mpproject.presentation.viewmodel.TaskViewModelFactory;
import com.example.mpproject.data.local.LocalSessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private ModuleViewModel moduleViewModel;
    private TaskViewModel taskViewModel;
    private CalendarViewModel calendarViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setGreeting();
        setDate();
        setupClicks();
        setupModulePreview();
        setupTodayTasks();
        setupComingUp();
    }

    private void setupClicks() {
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_settingsFragment));

        binding.btnNewTask.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.tasksFragment));

        binding.btnNewNote.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.notesFragment));

        binding.btnSeeAllTasks.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.tasksFragment));

        binding.btnSeeCalendar.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.calendarFragment));

        binding.btnTimer.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.timerFragment));

        binding.btnSeeAllModules.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.modulesFragment));

        binding.cardHomeModules.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.modulesFragment));

        binding.homeModuleChip1.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.modulesFragment));

        binding.homeModuleChip2.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.modulesFragment));

        binding.homeModuleChip3.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.modulesFragment));

        binding.homeModuleChip4.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.modulesFragment));
    }

    private void setupModulePreview() {
        String userId = LocalSessionManager.getCurrentUserId(requireContext());

        if (userId == null) {
            showModulePlaceholder("No user");
            return;
        }

        showModulePlaceholder("Loading...");

        AppDatabase db = AppDatabase.getDatabase(requireContext());

        ModuleViewModelFactory factory = new ModuleViewModelFactory(
                new ModuleRepositoryImpl(db.moduleDao()),
                userId
        );

        moduleViewModel = new ViewModelProvider(this, factory).get(ModuleViewModel.class);

        moduleViewModel.activeModules.observe(getViewLifecycleOwner(), this::populateHomeModules);
    }

    private void setupTodayTasks() {
        String userId = LocalSessionManager.getCurrentUserId(requireContext());

        if (userId == null) {
            showNoTodayTasks();
            return;
        }

        AppDatabase db = AppDatabase.getDatabase(requireContext());

        TaskViewModelFactory factory = new TaskViewModelFactory(
                new TodoRepositoryImpl(db.todoDao()),
                new ModuleRepositoryImpl(db.moduleDao()),
                userId
        );

        taskViewModel = new ViewModelProvider(this, factory).get(TaskViewModel.class);

        taskViewModel.allTodos.observe(getViewLifecycleOwner(), this::populateTodayTasks);
    }

    private void setupComingUp() {
        String userId = LocalSessionManager.getCurrentUserId(requireContext());

        if (userId == null) {
            showNoUpcoming();
            return;
        }

        AppDatabase db = AppDatabase.getDatabase(requireContext());

        CalendarViewModelFactory factory = new CalendarViewModelFactory(
                new CalendarEventRepositoryImpl(db.calendarEventDao()),
                new TodoRepositoryImpl(db.todoDao()),
                new ModuleRepositoryImpl(db.moduleDao()),
                userId
        );

        calendarViewModel = new ViewModelProvider(this, factory).get(CalendarViewModel.class);

        calendarViewModel.upcomingItems.observe(getViewLifecycleOwner(), this::populateComingUp);
    }

    private void populateTodayTasks(List<Todo> todos) {
        binding.containerTodayTasks.removeAllViews();

        if (todos == null || todos.isEmpty()) {
            showNoTodayTasks();
            return;
        }

        int added = 0;

        for (Todo todo : todos) {
            if (todo.getDueDate() != null && isToday(todo.getDueDate())) {
                binding.containerTodayTasks.addView(
                        createHomeRow(
                                todo.getTitle(),
                                formatTodoSubtitle(todo)
                        )
                );

                added++;

                if (added >= 3) {
                    break;
                }
            }
        }

        if (added == 0) {
            showNoTodayTasks();
        } else {
            binding.tvNoTasks.setVisibility(View.GONE);
            binding.containerTodayTasks.setVisibility(View.VISIBLE);
        }
    }

    private void populateComingUp(List<AgendaItem> items) {
        binding.containerUpcomingItems.removeAllViews();

        if (items == null || items.isEmpty()) {
            showNoUpcoming();
            return;
        }

        int added = 0;
        long now = System.currentTimeMillis();

        for (AgendaItem item : items) {
            if (item.getSortKey() < now) {
                continue;
            }

            String title;
            String subtitle;

            if (item.getType() == AgendaItem.TYPE_EVENT) {
                CalendarEvent event = item.getEvent();
                title = event.getTitle();
                subtitle = "Event • " + formatDateTime(event.getStartTime());
            } else {
                Todo todo = item.getTodo();
                title = todo.getTitle();
                subtitle = "Task due • " + formatDateTime(todo.getDueDate());
            }

            binding.containerUpcomingItems.addView(createHomeRow(title, subtitle));
            added++;

            if (added >= 3) {
                break;
            }
        }

        if (added == 0) {
            showNoUpcoming();
        } else {
            binding.tvNoUpcoming.setVisibility(View.GONE);
            binding.containerUpcomingItems.setVisibility(View.VISIBLE);
        }
    }

    private void showNoTodayTasks() {
        binding.containerTodayTasks.removeAllViews();
        binding.containerTodayTasks.setVisibility(View.GONE);
        binding.tvNoTasks.setVisibility(View.VISIBLE);
    }

    private void showNoUpcoming() {
        binding.containerUpcomingItems.removeAllViews();
        binding.containerUpcomingItems.setVisibility(View.GONE);
        binding.tvNoUpcoming.setVisibility(View.VISIBLE);
    }

    private TextView createHomeRow(String title, String subtitle) {
        Context context = requireContext();

        TextView row = new TextView(context);

        String safeTitle = title != null && !title.trim().isEmpty()
                ? title.trim()
                : "Untitled";

        String safeSubtitle = subtitle != null && !subtitle.trim().isEmpty()
                ? subtitle.trim()
                : "";

        if (safeSubtitle.isEmpty()) {
            row.setText(safeTitle);
        } else {
            row.setText(safeTitle + "\n" + safeSubtitle);
        }

        row.setTextSize(12);
        row.setTextColor(resolveThemeColor(context, android.R.attr.textColorPrimary));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_rounded_surface);
        row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        row.setSingleLine(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        row.setLayoutParams(params);

        return row;
    }

    private String formatTodoSubtitle(Todo todo) {
        StringBuilder subtitle = new StringBuilder();

        if (todo.getPriority() != null && !todo.getPriority().trim().isEmpty()) {
            subtitle.append(todo.getPriority()).append(" priority");
        }

        if (todo.getDueDate() != null) {
            if (subtitle.length() > 0) {
                subtitle.append(" • ");
            }

            subtitle.append("Due ").append(formatTime(todo.getDueDate()));
        }

        return subtitle.toString();
    }

    private boolean isToday(long millis) {
        Calendar item = Calendar.getInstance();
        item.setTimeInMillis(millis);

        Calendar today = Calendar.getInstance();

        return item.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && item.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    private String formatDateTime(Long millis) {
        if (millis == null) {
            return "";
        }

        Calendar item = Calendar.getInstance();
        item.setTimeInMillis(millis);

        Calendar today = Calendar.getInstance();

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        if (item.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && item.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "Today " + formatTime(millis);
        }

        if (item.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR)
                && item.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
            return "Tomorrow " + formatTime(millis);
        }

        return new SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    private void populateHomeModules(List<Module> modules) {
        TextView[] chips = {
                binding.homeModuleChip1,
                binding.homeModuleChip2,
                binding.homeModuleChip3,
                binding.homeModuleChip4
        };

        for (TextView chip : chips) {
            chip.setVisibility(View.GONE);
            chip.setText("");
        }

        if (modules == null || modules.isEmpty()) {
            showModulePlaceholder("No modules yet");
            return;
        }

        int count = Math.min(modules.size(), chips.length);

        for (int i = 0; i < count; i++) {
            chips[i].setText(getModuleChipText(modules.get(i)));
            chips[i].setVisibility(View.VISIBLE);
        }

        if (modules.size() > chips.length) {
            chips[chips.length - 1].setText("+" + (modules.size() - chips.length + 1) + " more");
            chips[chips.length - 1].setVisibility(View.VISIBLE);
        }
    }

    private void showModulePlaceholder(String text) {
        binding.homeModuleChip1.setText(text);
        binding.homeModuleChip1.setVisibility(View.VISIBLE);

        binding.homeModuleChip2.setVisibility(View.GONE);
        binding.homeModuleChip3.setVisibility(View.GONE);
        binding.homeModuleChip4.setVisibility(View.GONE);
    }

    private String getModuleChipText(Module module) {
        if (module == null) {
            return "MODULE";
        }

        if (module.getModuleCode() != null && !module.getModuleCode().trim().isEmpty()) {
            return module.getModuleCode().trim();
        }

        if (module.getName() != null && !module.getName().trim().isEmpty()) {
            String name = module.getName().trim();

            if (name.length() <= 8) {
                return name;
            }

            return name.substring(0, 8).toUpperCase(Locale.getDefault());
        }

        return "MODULE";
    }

    private int resolveThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        boolean resolved = context.getTheme().resolveAttribute(attr, typedValue, true);

        if (!resolved) {
            return android.graphics.Color.WHITE;
        }

        if (typedValue.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(context, typedValue.resourceId);
        }

        return typedValue.data;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        String timeOfDay;

        if (hour < 12) {
            timeOfDay = getString(R.string.greeting_morning);
        } else if (hour < 17) {
            timeOfDay = getString(R.string.greeting_afternoon);
        } else {
            timeOfDay = getString(R.string.greeting_evening);
        }

        binding.tvGreeting.setText(timeOfDay + ", Student");
    }

    private void setDate() {
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
        binding.tvDate.setText(date);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}