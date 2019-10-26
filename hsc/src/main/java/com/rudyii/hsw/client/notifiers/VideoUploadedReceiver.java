package com.rudyii.hsw.client.notifiers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.rudyii.hsw.client.R;

import java.util.HashMap;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_NORMAL;
import static com.rudyii.hsw.client.helpers.Utils.INFO_SOUND;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static java.util.Objects.requireNonNull;

public class VideoUploadedReceiver {
    public static void notifyAboutNewVideoUploaded(HashMap<String, Object> videoData) {
        Context context = getAppContext();
        String serverName = (String) videoData.get("serverName");
        String url = (String) videoData.get("url");
        String fileName = (String) videoData.get("fileName");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NORMAL)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_video_uploaded))
                .setContentText(fileName)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture((Bitmap) videoData.get("image")))
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
