package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_NORMAL;
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
 * Created by Jack on 19.12.2017.
 */

public class ServerStartupNotifier {
    public ServerStartupNotifier(Context context, HashMap<String, Object> startupData) {
        String serverName = (String) startupData.get("serverName");
        String serverPid = (String) startupData.get("pid");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NORMAL)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_server_started) + startupData.get("eventDateTime").toString())
                .setContentText(context.getResources().getString(R.string.notif_text_server_started_with_pid) + serverPid)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(Long.parseLong(startupData.get("eventId").toString()))
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
