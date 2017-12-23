package com.rudyii.hsw.client.listeners;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.rudyii.hsw.client.R;

import java.util.HashMap;
import java.util.Objects;

import static android.media.AudioManager.STREAM_NOTIFICATION;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
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

        if (motionData == null) {
            return;
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Motion detected on camera: " + motionData.get("cameraName"))
                .setContentText("Motion detected at " + getCurrentTimeAndDateDoubleDotsDelimFrom((Long) motionData.get("timeStamp")) + " with area " + motionData.get("motionArea") + "%")
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture((Bitmap) motionData.get("image")))
                .setAutoCancel(true);

        if (!Objects.equals(getStringValueFromSettings("CAMERA_APP"), "")) {
            PendingIntent launchCameraApp = PendingIntent.getActivity(context, 0,
                    context.getPackageManager().getLaunchIntentForPackage(getStringValueFromSettings("CAMERA_APP")), PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(launchCameraApp);
        }

        try {
            Uri soundUri = Uri.parse(getStringValueFromSettings("MOTION_SOUND"));
            Ringtone r = RingtoneManager.getRingtone(getAppContext(), soundUri);
            r.setStreamType(STREAM_NOTIFICATION);
            r.play();
        } catch (Exception e) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getAppContext(), soundUri);
            r.setStreamType(STREAM_NOTIFICATION);
            r.play();
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
