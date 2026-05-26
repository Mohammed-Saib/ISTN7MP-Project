package com.example.mpproject.presentation.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public final class ReminderScheduler {

    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private ReminderScheduler() {}

    public static void scheduleEventReminder(Context context,
                                             String eventId,
                                             String eventTitle,
                                             long startTimeMs,
                                             boolean allDay,
                                             String eventType) {
        if (context == null || eventId == null || startTimeMs <= System.currentTimeMillis()) return;

        long reminderTime = calculateEventReminderTime(startTimeMs, allDay, eventType);
        if (reminderTime <= System.currentTimeMillis()) return;

        String title = buildEventNotificationTitle(eventType);
        String safeTitle = eventTitle != null && !eventTitle.trim().isEmpty()
                ? eventTitle.trim()
                : "Your event";

        String message = safeTitle + (allDay ? " is today." : " is coming up soon.");

        scheduleOneTimeNotification(
                context.getApplicationContext(),
                requestCode("EVENT_" + eventId),
                reminderTime,
                title,
                message,
                "EVENT"
        );
    }

    public static void cancelEventReminder(Context context, String eventId) {
        if (context == null || eventId == null) return;
        cancelNotification(context.getApplicationContext(), requestCode("EVENT_" + eventId));
    }

    public static void scheduleTaskReminder(Context context,
                                            String todoId,
                                            String taskTitle,
                                            Long dueDateMs,
                                            String priority) {
        if (context == null || todoId == null || dueDateMs == null) return;
        if (dueDateMs <= System.currentTimeMillis()) return;

        long reminderTime = calculateTaskReminderTime(dueDateMs, priority);
        if (reminderTime <= System.currentTimeMillis()) return;

        String title;
        if ("HIGH".equals(priority)) {
            title = "High priority task due soon";
        } else {
            title = "Task reminder";
        }

        String safeTitle = taskTitle != null && !taskTitle.trim().isEmpty()
                ? taskTitle.trim()
                : "Your task";

        String message = safeTitle + " is due soon.";

        scheduleOneTimeNotification(
                context.getApplicationContext(),
                requestCode("TODO_" + todoId),
                reminderTime,
                title,
                message,
                "TODO"
        );
    }

    public static void cancelTaskReminder(Context context, String todoId) {
        if (context == null || todoId == null) return;
        cancelNotification(context.getApplicationContext(), requestCode("TODO_" + todoId));
    }

    public static void scheduleDailyStudyReminder(Context context) {
        if (context == null) return;

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 19);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        //testing:
//        next.set(Calendar.HOUR_OF_DAY, 16);
//        next.set(Calendar.MINUTE, 15);

        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        scheduleRepeatingNotification(
                context.getApplicationContext(),
                requestCode("DAILY_STUDY_REMINDER"),
                next.getTimeInMillis(),
                DAY_MS,
                "Academic Weapon check-in",
                "You have not studied yet? Open your notes and keep the streak alive 🔥",
                "DAILY_STUDY"
        );
    }

    //testing method for study motivation notifications
    public static void scheduleMotivationTestReminder(Context context, int delayMinutes) {
        if (context == null) return;

        long triggerAtMs = System.currentTimeMillis() + (delayMinutes * MINUTE_MS);

        scheduleOneTimeNotification(
                context.getApplicationContext(),
                requestCode("MOTIVATION_TEST_" + System.currentTimeMillis()),
                triggerAtMs,
                "Academic Weapon check-in",
                "This is a test motivation notification.",
                "DAILY_STUDY"
        );
    }


//    private static long calculateEventReminderTime(long startTimeMs, boolean allDay, String eventType) {
//        if (allDay) {
//            return startTimeMs + 9L * 60L * MINUTE_MS;
//        }
//
//        if ("EXAM".equals(eventType) || "ASSIGNMENT_DUE".equals(eventType)) {
//            return startTimeMs - 24L * 60L * MINUTE_MS;
//        }
//
//        return startTimeMs - 30L * MINUTE_MS;
//    }

    //testing
    private static long calculateEventReminderTime(long startTimeMs, boolean allDay, String eventType) {
        return startTimeMs - 1L * MINUTE_MS;
    }

//    private static long calculateTaskReminderTime(long dueDateMs, String priority) {
//        if ("HIGH".equals(priority)) {
//            return dueDateMs - 24L * 60L * MINUTE_MS;
//        }
//
//        if ("LOW".equals(priority)) {
//            return dueDateMs - 60L * MINUTE_MS;
//        }
//
//        return dueDateMs - 3L * 60L * MINUTE_MS;
//    }

    //testing
    private static long calculateTaskReminderTime(long dueDateMs, String priority) {
        return dueDateMs - 1L * MINUTE_MS;
    }

    private static String buildEventNotificationTitle(String eventType) {
        if ("EXAM".equals(eventType)) {
            return "Exam coming up";
        }

        if ("ASSIGNMENT_DUE".equals(eventType)) {
            return "Assignment deadline reminder";
        }

        if ("LECTURE".equals(eventType)) {
            return "Lecture reminder";
        }

        return "Upcoming event";
    }

    private static void scheduleOneTimeNotification(Context context,
                                                    int requestCode,
                                                    long triggerAtMs,
                                                    String title,
                                                    String message,
                                                    String type) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = buildPendingIntent(context, requestCode, title, message, type);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMs,
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMs,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                );
            }
        } catch (SecurityException e) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
            );
        }
    }

    private static void scheduleRepeatingNotification(Context context,
                                                      int requestCode,
                                                      long firstTriggerAtMs,
                                                      long intervalMs,
                                                      String title,
                                                      String message,
                                                      String type) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = buildPendingIntent(context, requestCode, title, message, type);

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                firstTriggerAtMs,
                intervalMs,
                pendingIntent
        );
    }

    private static void cancelNotification(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                new Intent(context, NotificationReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent buildPendingIntent(Context context,
                                                    int requestCode,
                                                    String title,
                                                    String message,
                                                    String type) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, requestCode);
        intent.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        intent.putExtra(NotificationReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(NotificationReceiver.EXTRA_TYPE, type);

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int requestCode(String key) {
        return key.hashCode() & 0x7fffffff;
    }
}