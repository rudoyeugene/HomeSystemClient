package com.rudyii.hsw.client.notifiers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.rudyii.hsw.client.R;

import java.util.Map;
import java.util.Objects;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_HIGH;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.readImageFromUrl;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static java.util.Objects.requireNonNull;

/**
 * Created by Jack on 16.12.2017.
 */

public class MotionDetectedNotifier {
    public static void notifyAboutMotionDetected(Map<String, String> motionData) {
        Context context = getAppContext();
        String serverName = motionData.get("serverName");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName)
                .setContentText(motionData.get("cameraName") + ": " + motionData.get("motionArea") + "%, " + getCurrentTimeAndDateDoubleDotsDelimFrom(Long.parseLong(motionData.get("timeStamp"))))
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(readImageFromUrl(motionData.get("imageUrl"))))
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 200, 200, 200, 200, 200})
                .setSound(Uri.parse(getStringValueFromSettings("MOTION_SOUND")), AudioManager.STREAM_NOTIFICATION);

        if (!Objects.equals(getStringValueFromSettings("CAMERA_APP"), "")) {
            PendingIntent launchCameraApp = PendingIntent.getActivity(context, 0,
                    context.getPackageManager().getLaunchIntentForPackage(getStringValueFromSettings("CAMERA_APP")), PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(launchCameraApp);
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
