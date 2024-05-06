package com.example.dissertation.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.dissertation.R;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "task_channel";
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String NOTIFICATION_ID_KEY = "notification_id";

    private final Context mContext;
    private final NotificationManager notificationManager;
    private final AlarmManager alarmManager;
    private final SharedPreferences sharedPreferences;

    public NotificationHelper(Context context) {
        this.mContext = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.sharedPreferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
    public void scheduleNotification(long triggerAtMillis, String title, String content) {
        int requestCode = getNextRequestId();
        Log.d(TAG, "Scheduling notification with request code: " + requestCode);

        Intent intent = new Intent(mContext, AlertReceiver.class);
        intent.setAction("com.example.dissertation.ALARM_ACTION");
        intent.putExtra("notification_id", requestCode);
        intent.putExtra("title", title);
        intent.putExtra("content", content);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
        }
        Log.d(TAG, "Alarm set with request code " + requestCode + " at " + triggerAtMillis);
    }

    private int getNextRequestId() {
        int lastId = sharedPreferences.getInt(NOTIFICATION_ID_KEY, 0);
        Log.d(TAG, "Last notification ID: " + lastId);
        int nextId = lastId + 1;
        Log.d(TAG, "Next notification ID generated before: " + nextId);
        sharedPreferences.edit().putInt(NOTIFICATION_ID_KEY, nextId).apply();
        Log.d(TAG, "Next notification ID generated after: " + nextId);
        return nextId;
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
}
