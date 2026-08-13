package com.example.mpproject;

import android.app.Application;

import com.example.mpproject.data.local.DemoDataSeeder;

// Application entry point. Kicks off demo data seeding before the first screen is drawn so a fresh
// install has an account to log into; the seeder itself is a no-op once the demo account exists.
public class AcademicWeaponApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DemoDataSeeder.seedIfNeeded(this);
    }
}
