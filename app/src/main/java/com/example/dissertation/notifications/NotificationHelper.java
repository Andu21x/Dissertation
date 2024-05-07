// Inspired by https://developer.android.com/develop/ui/views/notifications/time-sensitive
// and https://developer.android.com/reference/android/app/AlarmManager
// and https://developer.android.com/reference/android/app/NotificationManager

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
    private static final String TAG = "NotificationHelper"; // Tag used for easier identification of logs
    private static final String CHANNEL_ID = "task_channel";
    private static final String PREFS_NAME = "NotificationPrefs";

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
        initializeNotificationChannel();
    }

    // Stop creating notification channels if we already initialized them before
    @SuppressLint("ObsoleteSdkInt")
    private void initializeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Task Notifications", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications for task updates");
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            } else {
                Log.d(TAG, "Notification channel already exists");
            }
        }
    }

    @SuppressLint({"ObsoleteSdkInt"})
    public void scheduleNotification(long triggerAtMillis, String title, String content, String type) {
        // Use a unique request code for each notification to prevent duplicates
        int requestCode = generateRequestCode(title, triggerAtMillis);
        Log.d(TAG, "Scheduling notification with request code: " + requestCode);

        // Set up the intent and put in all the extra needed parameters
        Intent intent = new Intent(mContext, AlertReceiver.class);
        intent.setAction("com.example.dissertation.ALARM_ACTION");
        intent.putExtra("notification_id", requestCode);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("type", type);


        // Flagged as an urgent message
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0));

        // Accommodate for different versions of android within the try block
        // Find the right type of alarm to be set, preferably we would like to go top down
        // setExactAndAllowWhileIdle is the best but requires security handling and permissions
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
                // For a very old version of android just try the most basic alarm setter
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.d(TAG, "Alarm set with request code " + requestCode + " at " + triggerAtMillis);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to set exact alarm due to security restrictions", e);
        }
    }

    @SuppressLint("Range")
    public void scheduleWeatherNotifications(String city) {
        long currentTime = System.currentTimeMillis();
        long endTime = currentTime + (5 * 24 * 3600 * 1000); // 5 days ahead from current time
        Log.d(TAG, "Before the try block");

        // Query forecasts with high precipitation probability
        try (Cursor cursor = dbHelper.getHighPopForecasts(city, currentTime, endTime)) {
            if (cursor != null) {
                int delayIncrement = 0; // Introduce a manual delay to prevent alarm spamming
                Log.d("DatabaseHelper", "Number of entries fetched: " + cursor.getCount());
                if (cursor.moveToFirst()) {
                    do {
                        long dateTime = cursor.getLong(cursor.getColumnIndex("dateTime"));
                        String description = cursor.getString(cursor.getColumnIndex("weather_description"));


                        String title = "Weather Alert!";
                        String content = String.format(Locale.UK, "High chance of rain on %s. Details: %s",
                                new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.UK).format(new java.util.Date(dateTime * 1000)),
                                description);

                        // Schedule the notification, making sure to convert the notificationTime to milliseconds, as "triggerAtMillis" requires milliseconds
                        scheduleNotification(System.currentTimeMillis() + 5000 + (3000L * delayIncrement), title, content, "weather"); // Pass "weather" as the type
                        Log.d(TAG, "Schedule notification at: " + System.currentTimeMillis() + 5000 + (1500 * delayIncrement) +  " with title: " + title + " and content: " + content);

                        delayIncrement++; // Increase the delay increment for the next notification
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling weather notifications", e);
        }
    }


    public void sendTaskNotification(int notificationId, String title, String content) {
        Log.d(TAG, "sendTaskNotification ID at the top: " + notificationId);

        // Customize notification details specific to task
        // Heavily inspired by the android documentation
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Use the notification manager to post the notification
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification sent through the builder: " + title + " with ID " + notificationId);
    }

    public void sendWeatherNotification(int notificationId, String title, String content) {
        Log.d(TAG, "sendWeatherNotification ID at the top: " + notificationId);

        // Customize notification details specific to weather
        // Heavily inspired by the android documentation
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.weather_icon)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Use the notification manager to post the notification
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Weather notification sent: " + title);
    }

    // Method to generate a unique request code
    public int generateRequestCode(String identifier, long dateTime) {
        return (identifier + dateTime).hashCode();
    }
}
