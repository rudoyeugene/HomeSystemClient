package com.rudyii.hsw.client.listeners;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.rudyii.hsw.client.R;

import java.util.HashMap;
import java.util.Objects;

import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;

/**
 * Created by j-a-c on 16.12.2017.
 */

public class MotionListener extends BroadcastReceiver {
    public static String HSC_MOTION_DETECTED = "com.rudyii.hsw.client.HSC_MOTION_DETECTED";

    @Override
    public void onReceive(Context context, Intent intent) {
        HashMap<String, Object> motionData = (HashMap<String, Object>) intent.getSerializableExtra("HSC_MOTION_DETECTED");
        String serverName = (String) motionData.get("serverName");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_motion_detected_on_camera) + motionData.get("cameraName"))
                .setContentText(getCurrentTimeAndDateDoubleDotsDelimFrom((Long) motionData.get("timeStamp")) + context.getResources().getString(R.string.notif_text_area_size) + motionData.get("motionArea") + "%")
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture((Bitmap) motionData.get("image")))
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 200, 200, 200, 200, 200})
                .setSound(Uri.parse(getStringValueFromSettings("MOTION_SOUND")), AudioManager.STREAM_NOTIFICATION);

        if (!Objects.equals(getStringValueFromSettings("CAMERA_APP"), "")) {
            PendingIntent launchCameraApp = PendingIntent.getActivity(context, 0,
                    context.getPackageManager().getLaunchIntentForPackage(getStringValueFromSettings("CAMERA_APP")), PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(launchCameraApp);
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
