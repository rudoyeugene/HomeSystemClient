package com.rudyii.hsw.client.helpers;

import static android.os.Build.VERSION_CODES.O;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

public class NotificationChannelsBuilder {
    public static final String NOTIFICATION_CHANNEL_HIGH = "HSC_PopUp";
    public static final String NOTIFICATION_CHANNEL_NORMAL = "HSC_Notify";
    public static final String NOTIFICATION_CHANNEL_MUTED = "HSC_Muted";

    @RequiresApi(api = O)
    public static void createNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        NotificationChannel notificationChannelMuted = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_MUTED);
        if (notificationChannelMuted == null) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            notificationChannelMuted = new NotificationChannel(NOTIFICATION_CHANNEL_MUTED, NOTIFICATION_CHANNEL_MUTED, importance);
            notificationManager.createNotificationChannel(notificationChannelMuted);
        }

        NotificationChannel notificationChannelHigh = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_HIGH);
        if (notificationChannelHigh == null) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            notificationChannelHigh = new NotificationChannel(NOTIFICATION_CHANNEL_HIGH, NOTIFICATION_CHANNEL_HIGH, importance);
            notificationChannelHigh.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes);
            enableLightsAndVibration(notificationChannelHigh);
            notificationManager.createNotificationChannel(notificationChannelHigh);
        }

        NotificationChannel notificationChannelNormal = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_NORMAL);
        if (notificationChannelNormal == null) {
            int importance = NotificationManager.IMPORTANCE_HIGH; //Set the importance level
            notificationChannelNormal = new NotificationChannel(NOTIFICATION_CHANNEL_NORMAL, NOTIFICATION_CHANNEL_NORMAL, importance);
            notificationChannelNormal.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes);
            enableLightsAndVibration(notificationChannelNormal);
            notificationManager.createNotificationChannel(notificationChannelNormal);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void enableLightsAndVibration(NotificationChannel notificationChannelNormal) {
        notificationChannelNormal.enableLights(true);
        notificationChannelNormal.enableVibration(true);
    }

}
