package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_NORMAL;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.rudyii.hsw.client.R;

import java.util.HashMap;
import java.util.Objects;

public class VideoUploadedNotifier {
    public VideoUploadedNotifier(Context context, HashMap<String, Object> videoData) {
        String serverName = (String) videoData.get("serverName");
        String url = (String) videoData.get("url");
        String fileName = (String) videoData.get("fileName");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NORMAL)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_video_uploaded))
                .setContentText(fileName)
                .setStyle(new NotificationCompat.BigTextStyle().bigText((videoData.get("eventDateTime").toString())))
                .setAutoCancel(true)
                .setWhen(Long.parseLong(videoData.get("eventId").toString()))
                .setVibrate(new long[]{0, 200, 200, 200, 200, 200});

        if (!Objects.equals(getStringValueFromSettings("CAMERA_APP"), "")) {
            PendingIntent launchCameraApp = PendingIntent.getActivity(context, 0,
                    context.getPackageManager().getLaunchIntentForPackage(getStringValueFromSettings("CAMERA_APP")), PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(launchCameraApp);
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
