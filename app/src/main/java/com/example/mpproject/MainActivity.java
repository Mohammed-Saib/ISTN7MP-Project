package com.example.mpproject;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.AssessmentRepositoryImpl;
import com.example.mpproject.data.repository.CalendarEventRepositoryImpl;
import com.example.mpproject.data.repository.ModuleNoteRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.PersonalNoteRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// [View] Single-activity host — owns the NavController and bottom nav visibility.
public class MainActivity extends AppCompatActivity {

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
        super.onCreate(savedInstanceState);

        // Allow content to draw behind system bars (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Auth and settings screens hide the bottom nav
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean hideNav = id == R.id.loginFragment
                    || id == R.id.registerFragment
                    || id == R.id.settingsFragment;
            bottomNav.setVisibility(hideNav ? View.GONE : View.VISIBLE);
        });

        // On a fresh launch (not a rotation), sign out if the user didn't check "Remember me".
        // savedInstanceState is non-null on rotation, so this only fires on cold starts.
        if (savedInstanceState == null) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            if (!prefs.getBoolean("pref_remember_me", false)) {
                FirebaseAuth.getInstance().signOut();
            }
        }

        // Already signed in — jump straight to the home dashboard and pull latest data from Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            navController.navigate(R.id.homeFragment);
            restoreFromFirestore(currentUser.getUid());
        }
    }
}
