package com.rudyii.hsw.client.listeners;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;

/**
 * Created by j-a-c on 18.12.2017.
 */

public class OfflineDeviceListener extends BroadcastReceiver {
    public static String HSC_DEVICE_REBOOT = "com.rudyii.hsw.client.HSC_DEVICE_REBOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String offlineDevice = (String) intent.getSerializableExtra("HSC_DEVICE_REBOOT");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Camera " + offlineDevice + " is rebooting")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500})
                .setSound(Uri.parse(getStringValueFromSettings("INFO_SOUND")), AudioManager.STREAM_NOTIFICATION);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
