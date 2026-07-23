package com.example.mpproject.presentation.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.mpproject.MainActivity;
import android.annotation.SuppressLint;

import com.example.mpproject.R;

public final class NotificationHelper {

    public static final String CHANNEL_REMINDERS = "academic_reminders";
    public static final String CHANNEL_STUDY = "study_motivation";

    private NotificationHelper() {}

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel remindersChannel = new NotificationChannel(
                CHANNEL_REMINDERS,
                "Event and Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        remindersChannel.setDescription("Reminders for calendar events, deadlines and tasks.");

        NotificationChannel studyChannel = new NotificationChannel(
                CHANNEL_STUDY,
                "Study Motivation",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        studyChannel.setDescription("Daily study nudges and Academic Weapon reminders.");

        manager.createNotificationChannel(remindersChannel);
        manager.createNotificationChannel(studyChannel);
    }

    public static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public static void showReminderNotification(Context context,
                                                int notificationId,
                                                String title,
                                                String message,
                                                String type) {
        createNotificationChannels(context);

        if (!canPostNotifications(context)) return;

        String channelId = "DAILY_STUDY".equals(type) ? CHANNEL_STUDY : CHANNEL_REMINDERS;

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}
