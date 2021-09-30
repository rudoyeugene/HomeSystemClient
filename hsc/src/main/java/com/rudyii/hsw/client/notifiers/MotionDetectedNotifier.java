package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_HIGH;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getLooper;
import static com.rudyii.hsw.client.helpers.Utils.readImageFromUrl;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.rudyii.hs.common.objects.logs.MotionLog;
import com.rudyii.hsw.client.BuildConfig;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Jack on 16.12.2017.
 */

public class MotionDetectedNotifier {
    public MotionDetectedNotifier(Context context, MotionLog motionLog, String serverAlias, long when) {
        new Handler(getLooper()).post(() -> {
            Bitmap motionImage = readImageFromUrl(motionLog.getImageUrl());
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setContentTitle(serverAlias)
                    .setContentText(String.format(currentLocale, "%s: %d %%, %s",
                            motionLog.getCameraName(),
                            motionLog.getMotionArea(),
                            getCurrentTimeAndDateDoubleDotsDelimFrom(when)))
                    .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(motionImage))
                    .setAutoCancel(true)
                    .setWhen(when)
                    .setVibrate(new long[]{0, 200, 200, 200, 200, 200});

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(motionLog.getImageUrl()));
            File outputDir = getAppContext().getCacheDir();
            try {
                File outputFile = File.createTempFile(String.valueOf(when), ".jpg", outputDir);
                if (outputFile.length() == 0) {
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    motionImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                }
                intent = new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(getAppContext(), BuildConfig.APPLICATION_ID + ".provider", outputFile));
            } catch (IOException e) {
                new ToastDrawer().showToast("Failed to load image");
                e.printStackTrace();
            } finally {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            mBuilder.setContentIntent(PendingIntent.getActivity(getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
        });
    }
}
