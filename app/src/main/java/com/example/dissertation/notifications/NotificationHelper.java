package com.example.dissertation.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.dissertation.R;
import com.example.dissertation.DatabaseHelper;

import java.util.Locale;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "task_channel";
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String NOTIFICATION_ID_KEY = "notification_id";

    private final Context mContext;
    private final NotificationManager notificationManager;
    private final AlarmManager alarmManager;
    private final SharedPreferences sharedPreferences;
    private final DatabaseHelper dbHelper;

    public NotificationHelper(Context context) {
        this.mContext = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.sharedPreferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dbHelper = new DatabaseHelper(context); // Initialize dbHelper
        createNotificationChannel();
    }

    @SuppressLint("ObsoleteSdkInt")
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Task Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for task updates");
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    @SuppressLint({"ObsoleteSdkInt"})
    public void scheduleNotification(long triggerAtMillis, String title, String content, String type) {
        // Use a unique request code for each notification to prevent duplicates
        int requestCode = generateRequestCode(title, triggerAtMillis);
        if (!checkAndStoreNotification(title, triggerAtMillis)) {
            Log.d(TAG, "Notification is a duplicate and will not be scheduled.");
            return;
        }
        Log.d(TAG, "Scheduling notification with request code: " + requestCode);

        Intent intent = new Intent(mContext, AlertReceiver.class);
        intent.setAction("com.example.dissertation.ALARM_ACTION");
        intent.putExtra("notification_id", requestCode);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("type", type);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    // Prompt the user to open the settings and allow exact alarms
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(settingsIntent);
                    Log.d(TAG, "Requesting permission to schedule exact alarms");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.d(TAG, "Alarm set with request code " + requestCode + " at " + triggerAtMillis);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to set exact alarm due to security restrictions", e);
        }
    }

    @SuppressLint("Range")
    public void scheduleWeatherNotifications() {
        long currentTime = System.currentTimeMillis() / 1000; // Current Unix time in seconds
        long endTime = currentTime + (10 * 24 * 3600); // 10 days ahead from current time

        // Query forecasts with high precipitation probability
        try (Cursor cursor = dbHelper.getHighPopForecasts(currentTime, endTime)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long dateTime = cursor.getLong(cursor.getColumnIndex("dateTime"));
                    double pop = cursor.getDouble(cursor.getColumnIndex("pop")); // Probability of precipitation
                    String description = cursor.getString(cursor.getColumnIndex("weather_description"));

                    if (dateTime > currentTime && pop > 50) {
                        long notificationTime = dateTime - (24 * 3600); // Schedule 24 hours before the event
                        String title = "Weather Alert!";
                        String content = String.format(Locale.UK, "High chance of rain on %s. Details: %s",
                                new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.UK).format(new java.util.Date(dateTime * 1000)),
                                description);

                        // Schedule the notification
                        scheduleNotification(notificationTime * 1000, title, content, "weather"); // Pass "weather" as the type
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling weather notifications", e);
        }
    }

    public void sendTaskNotification(int notificationId, String title, String content) {
        Log.d(TAG, "sendTaskNotification ID at the top: " + notificationId);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification sent through the builder: " + title + " with ID " + notificationId);
    }

    public void sendWeatherNotification(int notificationId, String title, String content) {
        // Customize notification details specific to weather
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.weather_icon)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Weather notification sent: " + title);
    }

    // Method to generate a unique request code
    public int generateRequestCode(String city, long dateTime) {
        String uniqueString = city + dateTime;
        return uniqueString.hashCode();
    }

    // Method to check for existing notifications and decide whether to schedule a new one
    public boolean checkAndStoreNotification(String city, long dateTime) {
        SharedPreferences prefs = mContext.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        String lastNotificationKey = city + "_lastNotification";
        long lastNotificationTime = prefs.getLong(lastNotificationKey, 0);
        long oneDayMillis = 24 * 3600 * 1000; // 24 hours in milliseconds

        if (dateTime > lastNotificationTime + oneDayMillis) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(lastNotificationKey, dateTime);
            editor.apply();
            return true; // Proceed with scheduling
        }
        return false; // Skip as it's a duplicate
    }


}
