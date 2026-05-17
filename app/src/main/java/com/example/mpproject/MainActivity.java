package com.example.mpproject;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.repository.CalendarEventRepositoryImpl;
import com.example.mpproject.data.repository.ModuleRepositoryImpl;
import com.example.mpproject.data.repository.TodoRepositoryImpl;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// [View] Single-activity host — owns the NavController and bottom nav visibility.
public class MainActivity extends AppCompatActivity {

    // Restore modules, todos, and calendar events from Firestore if Room is empty for this user.
    // Guards against data loss caused by DB migration wipes or clean app reinstalls.
    private void restoreFromFirestore(String uid) {
        AppDatabase db = AppDatabase.getDatabase(this);
        new ModuleRepositoryImpl(db.moduleDao()).syncFromFirestoreIfEmpty(uid);
        new TodoRepositoryImpl(db.todoDao()).syncFromFirestoreIfEmpty(uid);
        new CalendarEventRepositoryImpl(db.calendarEventDao()).syncFromFirestoreIfEmpty(uid);
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

        // Already signed in — jump straight to the home dashboard and restore any lost data
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            navController.navigate(R.id.homeFragment);
            restoreFromFirestore(currentUser.getUid());
        }
    }
}
