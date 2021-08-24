package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_HIGH;
import static com.rudyii.hsw.client.helpers.Utils.readImageFromUrl;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.rudyii.hsw.client.R;

import java.util.Map;
import java.util.Objects;

/**
 * Created by Jack on 16.12.2017.
 */

public class MotionDetectedNotifier {
    public MotionDetectedNotifier(Context context, Map<String, Object> motionData) {
        String serverName = motionData.get("serverName").toString();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName)
                .setContentText(motionData.get("cameraName") + ": " + motionData.get("motionArea") + "%, " + motionData.get("eventDateTime").toString())
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(readImageFromUrl(motionData.get("imageUrl").toString())))
                .setAutoCancel(true)
                .setWhen(Long.parseLong(motionData.get("eventId").toString()))
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
