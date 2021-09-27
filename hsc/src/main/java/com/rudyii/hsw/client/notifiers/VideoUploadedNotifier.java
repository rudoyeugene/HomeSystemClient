package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_NORMAL;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.saveDataFromUrl;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.rudyii.hs.common.objects.message.VideoUploadedMessage;
import com.rudyii.hsw.client.BuildConfig;
import com.rudyii.hsw.client.R;

import java.io.File;

public class VideoUploadedNotifier {
    public VideoUploadedNotifier(Context context, VideoUploadedMessage videoUploadedMessage) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NORMAL)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(String.format(currentLocale, "%s: %s",
                        videoUploadedMessage.getServerAlias(),
                        context.getResources().getString(R.string.notif_text_video_uploaded)))
                .setContentText(videoUploadedMessage.getFileName())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getCurrentTimeAndDateDoubleDotsDelimFrom(videoUploadedMessage.getPublishedAt())))
                .setAutoCancel(true)
                .setWhen(videoUploadedMessage.getPublishedAt())
                .setVibrate(new long[]{0, 200, 200, 200, 200, 200});

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUploadedMessage.getVideoUrl()));
        File outputDir = getAppContext().getCacheDir();
        try {
            File outputFile = File.createTempFile(String.valueOf(videoUploadedMessage.getPublishedAt()), ".mp4", outputDir);
            if (outputFile.length() == 0) {
                saveDataFromUrl(videoUploadedMessage.getVideoUrl(), outputFile);
            }
            intent = new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(getAppContext(), BuildConfig.APPLICATION_ID + ".provider", outputFile));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        mBuilder.setContentIntent(PendingIntent.getActivity(getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
