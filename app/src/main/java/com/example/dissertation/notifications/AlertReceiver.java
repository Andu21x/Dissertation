package com.example.dissertation.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlertReceiver", "Alarm received!");

        // Retrieve the notification ID
        int notificationId = intent.getIntExtra("notification_id", 0);  // Default to 0 if not found
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        Log.d("AlertReceiver", "Received with title: " + title + " and content: " + content); // Log details
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.sendTaskNotification(notificationId, title, content);  // Use the retrieved ID
    }
}
