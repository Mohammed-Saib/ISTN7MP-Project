package com.example.mpproject;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

// [View] Single-activity host. Sets up navigation and bottom nav visibility rules.
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Hide bottom nav on auth screens
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean isAuthScreen = destination.getId() == R.id.loginFragment
                    || destination.getId() == R.id.registerFragment;
            bottomNavigationView.setVisibility(isAuthScreen ? View.GONE : View.VISIBLE);
        });

        // Skip login screen if the user is already authenticated
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            navController.navigate(R.id.tasksFragment);
        }
    }
}
