package com.fimbleenterprises.whereuat.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.fimbleenterprises.whereuat.MainActivity;
import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.R;

import androidx.core.app.NotificationCompat;

public class MyNotificationManager {

    Context appContext = MyApp.getAppContext();

    private static final String TAG = "NotificationHelper";

    // Channels
    public static final String LOW_PRIORITY_CHANNELID = "LOW_PRIORITY_CHANNELID";
    public static final String HIGH_PRIORITY_CHANNELID = "HIGH_PRIORITY_CHANNELID";

    public NotificationChannel LOW_IMPORTANCE_CHANNEL;
    public NotificationChannel HIGH_IMPORTANCE_CHANNEL;

    // Specific notification ids
    public static final int HIGH_REQUESTID = 1777;
    public static final int LOW_REQUESTID = 1778;

    public static final int LOW_IMPORTANCE = NotificationManager.IMPORTANCE_LOW;
    public static final int HIGH_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;

    public static final String IS_MESSAGE = "ISMESSAGE";

    public final NotificationManager mNotificationManager;

    // The actual notification the user is actually fucking seeing if they go up there and look
    public Notification mHighNotification;
    public Notification mLowNotification;

    public MyNotificationManager() {
        this.mNotificationManager = MyApp.getAppContext().getSystemService(NotificationManager.class);
        createLowImportanceChannel();
        createHighImportanceChannel();
    }

    public boolean lowPriorityChannelExists() {
        return mNotificationManager.getNotificationChannel(LOW_PRIORITY_CHANNELID) != null;
    }

    public boolean highPriorityChannelExists() {
        return mNotificationManager.getNotificationChannel(HIGH_PRIORITY_CHANNELID) != null;
    }

    public void showLowPriorityNotification() {
        showLowPriorityNotification("Where You At?!", "This is a low priority message!");
    }

    public void showLowPriorityNotification(String title, String message) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, LOW_REQUESTID, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, LOW_PRIORITY_CHANNELID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(LOW_IMPORTANCE)

                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        this.mLowNotification = builder.build();

        mNotificationManager.notify(1, this.mLowNotification);
    }

    public void showHighPriorityNotification(String title, String message) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, HIGH_REQUESTID, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, HIGH_PRIORITY_CHANNELID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(LOW_IMPORTANCE)

                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        this.mHighNotification = builder.build();

        mNotificationManager.notify(1, mHighNotification);
    }

    public void showMessageReceivedNotification(String title, String message) {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.setAction(IS_MESSAGE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(IS_MESSAGE, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, HIGH_REQUESTID, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, HIGH_PRIORITY_CHANNELID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(LOW_IMPORTANCE)

                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        this.mHighNotification = builder.build();

        mNotificationManager.notify(1, mHighNotification);
    }

    public void showHighPriorityNotification() {
        showHighPriorityNotification("Where You At?!", "This is a fucking high priority message, ya fuck!");
    }

    private void createLowImportanceChannel() {

        // NotificationManger cannot be null
        assert mNotificationManager != null;

        if (mNotificationManager.getNotificationChannel(LOW_PRIORITY_CHANNELID) != null) {
            Log.i(TAG, "createLowImportanceChannel - Channel already exists, will not recreate.");
            return;
        }

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = MyApp.getAppContext().getString(R.string.low_priority_notification_channel_name);
            String description = MyApp.getAppContext().getString(R.string.low_priority_notification_channel_description);
            LOW_IMPORTANCE_CHANNEL = new NotificationChannel(LOW_PRIORITY_CHANNELID, name, LOW_IMPORTANCE);
            LOW_IMPORTANCE_CHANNEL.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            mNotificationManager.createNotificationChannel(LOW_IMPORTANCE_CHANNEL);
        }
    }

    private void createHighImportanceChannel() {

        // NotificationManger cannot be null
        assert mNotificationManager != null;

        if (mNotificationManager.getNotificationChannel(HIGH_PRIORITY_CHANNELID) != null) {
            Log.i(TAG, "createHighImportanceChannel - Channel already exists, will not recreate.");
            return;
        }

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = MyApp.getAppContext().getString(R.string.high_priority_notification_channel_name);
            String description = MyApp.getAppContext().getString(R.string.high_priority_notification_channel_description);
            HIGH_IMPORTANCE_CHANNEL = new NotificationChannel(HIGH_PRIORITY_CHANNELID, name, HIGH_IMPORTANCE);
            HIGH_IMPORTANCE_CHANNEL.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            mNotificationManager.createNotificationChannel(HIGH_IMPORTANCE_CHANNEL);
        }
    }


}
