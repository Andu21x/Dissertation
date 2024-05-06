package com.example.dissertation.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlertReceiver", "Alarm received!");

        int notificationId = intent.getIntExtra("notification_id", 0);
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        String type = intent.getStringExtra("type");  // "task" or "weather"

        Log.d("AlertReceiver", "Received with title: " + title + ", content: " + content + ", type: " + type);

        NotificationHelper notificationHelper = new NotificationHelper(context);
        if ("weather".equals(type)) {
            notificationHelper.sendWeatherNotification(notificationId, title, content);
        } else {
            notificationHelper.sendTaskNotification(notificationId, title, content);
        }
    }
}
