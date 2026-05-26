package com.example.mpproject.presentation.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Random;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_TYPE = "extra_type";

    private static final String[] MOTIVATION_TITLES = {
            "Academic Weapon check-in",
            "Your streak is calling",
            "Tiny study session?",
            "Your notes miss you",
            "Future you is watching",
            "Study streak reminder",
            "Quick revision time",
            "Do not lose momentum",
            "Your brain wants XP",
            "One small win today"
    };

    private static final String[] MOTIVATION_MESSAGES = {
            "Open your notes for 10 minutes and keep the streak alive 🔥",
            "You have not studied yet today. One small session still counts.",
            "Your academic weapon status is under review 😭 Revise something quick.",
            "Just one task or one note. That is enough to stay consistent.",
            "Future you will thank you for opening the app today.",
            "Your study streak is in danger. Save it before the day ends.",
            "A short revision now is better than panic later.",
            "You are one tap away from being productive.",
            "Do one small thing: check a task, open a note, or revise a module.",
            "Consistency beats cramming. Do a quick study check-in."
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra(
                EXTRA_NOTIFICATION_ID,
                (int) (System.currentTimeMillis() % Integer.MAX_VALUE)
        );

        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String type = intent.getStringExtra(EXTRA_TYPE);

        if ("DAILY_STUDY".equals(type)) {
            int index = new Random().nextInt(MOTIVATION_MESSAGES.length);
            title = MOTIVATION_TITLES[index];
            message = MOTIVATION_MESSAGES[index];
        }

        if (title == null || title.trim().isEmpty()) {
            title = "Study reminder";
        }

        if (message == null || message.trim().isEmpty()) {
            message = "Open the app and stay on track.";
        }

        NotificationHelper.showReminderNotification(context, notificationId, title, message, type);
    }
}