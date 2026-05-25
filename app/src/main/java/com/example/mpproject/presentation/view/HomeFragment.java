package com.example.mpproject.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mpproject.R;
import com.example.mpproject.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// [View] Home dashboard — greeting, daily progress placeholder, quick-capture buttons,
//        today's tasks and upcoming events sections (data wired up in future tasks).
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

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

        // Navigate to SettingsFragment when the gear icon is tapped
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_settingsFragment));

        // Quick-capture navigates to the respective tab
        binding.btnNewTask.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.tasksFragment));
        binding.btnNewNote.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.notesFragment));

        // "See all" links navigate to the appropriate tab
        binding.btnSeeAllTasks.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.tasksFragment));
        binding.btnSeeCalendar.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.calendarFragment));

        binding.btnTimer.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.timerFragment));
    }

    // Builds a time-aware greeting using the signed-in user's display name or a generic fallback
    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeOfDay;
        if (hour < 12)      timeOfDay = getString(R.string.greeting_morning);
        else if (hour < 17) timeOfDay = getString(R.string.greeting_afternoon);
        else                timeOfDay = getString(R.string.greeting_evening);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Use the display name if set; otherwise just show the time-of-day greeting
        String name = (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? ", " + user.getDisplayName()
                : "";

        binding.tvGreeting.setText(timeOfDay + name);
    }

    // Formats and displays today's date under the greeting
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
