package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_HIGH;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

import java.util.HashMap;

/**
 * Created by Jack on 18.12.2017.
 */

public class WanInfoNotifier {
    public WanInfoNotifier(Context context, HashMap<String, Object> wanInfoData) {
        String serverName = (String) wanInfoData.get("serverName");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_isp_changed))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getResources().getString(R.string.notif_text_current_isp)
                                + wanInfoData.get("isp")
                                + context.getResources().getString(R.string.notif_text_current_ip)
                                + wanInfoData.get("ip")))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(Long.parseLong(wanInfoData.get("eventId").toString()))
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
