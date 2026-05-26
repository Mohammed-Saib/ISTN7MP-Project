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
import com.example.mpproject.data.repository.AssessmentRepositoryImpl;
import com.example.mpproject.data.repository.CalendarEventRepositoryImpl;
import com.example.mpproject.data.repository.ModuleNoteRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.PersonalNoteRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.mpproject.presentation.view.ChatBotBottomSheet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mpproject.presentation.notification.NotificationHelper;
import com.example.mpproject.presentation.notification.ReminderScheduler;

// [View] Single-activity host — owns the NavController and bottom nav visibility.
public class MainActivity extends AppCompatActivity {

    private boolean syncingBottomNav = false;
    private static final int REQUEST_POST_NOTIFICATIONS = 5001;


    // Unconditional merge-sync from Firestore for all data types.
    // Runs on startup so that items added on another device are pulled in.
    // All FK children (assessments, module notes, todos, calendar events, personal notes) sync
    // only after modules are committed to Room — Room enforces PRAGMA foreign_keys = ON so any
    // insert with a non-null moduleId fails silently if the parent module row isn't present yet.
    private void restoreFromFirestore(String uid) {
        AppDatabase db = AppDatabase.getDatabase(this);

        new ModuleRepositoryImpl(db.moduleDao()).syncFromFirestore(uid, () -> {
            new AssessmentRepositoryImpl(db.assessmentDao()).syncFromFirestore(uid);
            new ModuleNoteRepositoryImpl(db.moduleNoteDao()).syncFromFirestore(uid);
            new PersonalNoteRepositoryImpl(db.personalNoteDao()).syncFromFirestore(uid);
            new TodoRepositoryImpl(db.todoDao()).syncFromFirestore(uid);
            new CalendarEventRepositoryImpl(db.calendarEventDao()).syncFromFirestore(uid);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedThemeMode();
        super.onCreate(savedInstanceState);

        // Allow content to draw behind system bars (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);
        NotificationHelper.createNotificationChannels(this);
        requestNotificationPermissionIfNeeded();

        //testing
        //l trigger a motivation notification 1 minute after the app opens
        ReminderScheduler.scheduleMotivationTestReminder(this, 1);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabChatbot = findViewById(R.id.fab_chatbot);

        fabChatbot.setOnClickListener(v ->
                new ChatBotBottomSheet().show(getSupportFragmentManager(), "chatbot"));

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();

        /*
         * We are NOT using NavigationUI.setupWithNavController here.
         *
         * Reason:
         * You have a special flow:
         * Modules -> Module Details -> Notes filtered by module.
         *
         * NavigationUI sometimes leaves the bottom nav thinking Modules is still selected,
         * so tapping Modules again does nothing.
         *
         * This manual handler fixes both selected and reselected tab clicks.
         */
        bottomNav.setOnItemSelectedListener(item -> {
            if (syncingBottomNav) return true;

            navigateToBottomTab(navController, item.getItemId());
            return true;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            if (syncingBottomNav) return;

            navigateToBottomTab(navController, item.getItemId());
        });

        // Auth and settings screens hide the bottom nav.
        // Also manually keeps the correct bottom nav item checked.
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

        // On a fresh launch (not a rotation), sign out if the user didn't check "Remember me".
        // savedInstanceState is non-null on rotation, so this only fires on cold starts.
        if (savedInstanceState == null) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

            if (!prefs.getBoolean("pref_remember_me", false)) {
                FirebaseAuth.getInstance().signOut();
            }
        }

        // Already signed in — jump straight to the home dashboard and pull latest data from Firestore.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            navController.navigate(R.id.homeFragment);
            restoreFromFirestore(currentUser.getUid());
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
            /*
             * Important:
             * When tapping Notes directly from bottom nav, open normal NotesFragment
             * with NO moduleId argument, so it does not stay filtered to the previous module.
             */
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