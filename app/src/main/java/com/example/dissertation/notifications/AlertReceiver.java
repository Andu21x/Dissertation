// Class extending BroadcastReceiver

package com.example.dissertation.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Feedback that alarm was correctly received
        Log.d("AlertReceiver", "Alarm received!");

        // Retrieve all extended data from the intent
        int notificationId = intent.getIntExtra("notification_id", 0);
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        String type = intent.getStringExtra("type");  // "task" or "weather"

        // Feedback log to verify that we send the right data
        Log.d("AlertReceiver", "Received with title: " + title + ", content: " + content + ", type: " + type);

        // Bring the notification helper into scope
        NotificationHelper notificationHelper = new NotificationHelper(context);

        // Check which type of notification it is and send the correct method
        if ("weather".equals(type)) {
            notificationHelper.sendWeatherNotification(notificationId, title, content);
        } else {
            notificationHelper.sendTaskNotification(notificationId, title, content);
        }
    }
}
