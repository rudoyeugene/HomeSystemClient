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

import static com.rudyii.hsw.client.helpers.Utils.INFO_SOUND;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static java.util.Objects.requireNonNull;

public class VideoUploadedListener extends BroadcastReceiver {
    public static final String HSC_VIDEO_UPLOADED = "com.rudyii.hsw.client.HSC_VIDEO_UPLOADED";

    @Override
    public void onReceive(Context context, Intent intent) {
        @SuppressWarnings("unchecked") HashMap<String, Object> motionData = (HashMap<String, Object>) intent.getSerializableExtra("HSC_VIDEO_UPLOADED");
        String serverName = (String) motionData.get("serverName");
        String url = (String) motionData.get("url");
        String fileName = (String) motionData.get("fileName");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_video_uploaded))
                .setContentText(fileName)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture((Bitmap) motionData.get("image")))
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 200, 200, 200, 200, 200})
                .setSound(Uri.parse(getStringValueFromSettings(INFO_SOUND)), AudioManager.STREAM_NOTIFICATION);

        Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        resultIntent.setData(Uri.parse(url));

        PendingIntent openUrlIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(openUrlIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());

    }
}
