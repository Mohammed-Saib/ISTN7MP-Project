package com.example.mpproject;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.LocalSessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.mpproject.presentation.view.ChatBotBottomSheet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mpproject.presentation.notification.NotificationHelper;
import com.example.mpproject.presentation.notification.ReminderScheduler;

public class MainActivity extends AppCompatActivity {

    private boolean syncingBottomNav = false;
    private static final int REQUEST_POST_NOTIFICATIONS = 5001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedThemeMode();
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);
        NotificationHelper.createNotificationChannels(this);
        requestNotificationPermissionIfNeeded();

        ReminderScheduler.scheduleMotivationTestReminder(this, 1);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabChatbot = findViewById(R.id.fab_chatbot);

        fabChatbot.setOnClickListener(v ->
                new ChatBotBottomSheet().show(getSupportFragmentManager(), "chatbot"));

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();

        bottomNav.setOnItemSelectedListener(item -> {
            if (syncingBottomNav) return true;

            navigateToBottomTab(navController, item.getItemId());
            return true;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            if (syncingBottomNav) return;

            navigateToBottomTab(navController, item.getItemId());
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();

            boolean hideNav = id == R.id.loginFragment
                    || id == R.id.registerFragment
                    || id == R.id.settingsFragment;

            bottomNav.setVisibility(hideNav ? View.GONE : View.VISIBLE);
            fabChatbot.setVisibility(hideNav ? View.GONE : View.VISIBLE);

            int matchingBottomTabId = getMatchingBottomTabId(id);

            if (matchingBottomTabId != 0) {
                syncingBottomNav = true;

                if (bottomNav.getMenu().findItem(matchingBottomTabId) != null) {
                    bottomNav.getMenu().findItem(matchingBottomTabId).setChecked(true);
                }

                syncingBottomNav = false;
            }
        });

        if (savedInstanceState == null) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

            if (!prefs.getBoolean("pref_remember_me", false)) {
                LocalSessionManager.clearSession(this);
            }
        }

        String currentUser = LocalSessionManager.getCurrentUserId(this);

        if (currentUser != null) {
            navController.navigate(R.id.homeFragment);
            ReminderScheduler.scheduleDailyStudyReminder(this);
        }
    }

    private void applySavedThemeMode() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("pref_dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS
            );
        }
    }
    private int getMatchingBottomTabId(int destinationId) {
        if (destinationId == R.id.homeFragment) {
            return R.id.homeFragment;
        }

        if (destinationId == R.id.modulesFragment
                || destinationId == R.id.moduleDetailFragment) {
            return R.id.modulesFragment;
        }

        if (destinationId == R.id.notesFragment) {
            return R.id.notesFragment;
        }

        if (destinationId == R.id.tasksFragment) {
            return R.id.tasksFragment;
        }

        if (destinationId == R.id.calendarFragment) {
            return R.id.calendarFragment;
        }

        return 0;
    }

    private void navigateToBottomTab(NavController navController, int tabId) {
        if (tabId == R.id.homeFragment) {
            navigateToRootDestination(navController, R.id.homeFragment);
            return;
        }

        if (tabId == R.id.modulesFragment) {
            navigateToRootDestination(navController, R.id.modulesFragment);
            return;
        }

        if (tabId == R.id.notesFragment) {
            navigateFreshDestination(navController, R.id.notesFragment);
            return;
        }

        if (tabId == R.id.tasksFragment) {
            navigateToRootDestination(navController, R.id.tasksFragment);
            return;
        }

        if (tabId == R.id.calendarFragment) {
            navigateToRootDestination(navController, R.id.calendarFragment);
        }
    }

    private void navigateToRootDestination(NavController navController, int destinationId) {
        boolean popped = navController.popBackStack(destinationId, false);

        if (!popped) {
            navController.navigate(destinationId);
        }
    }

    private void navigateFreshDestination(NavController navController, int destinationId) {
        navController.popBackStack(destinationId, true);
        navController.navigate(destinationId);
    }
}
