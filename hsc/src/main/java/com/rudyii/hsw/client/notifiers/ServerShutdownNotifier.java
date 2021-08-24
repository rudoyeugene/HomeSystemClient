package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_HIGH;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Jack on 28.12.2017.
 */

public class ServerShutdownNotifier {
    public ServerShutdownNotifier(Context context, HashMap<String, Object> shutdownData) {
        String serverName = (String) shutdownData.get("serverName");
        Date date = new Date(Long.parseLong(shutdownData.get("eventId").toString()));

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_server_stopped))
                .setContentText(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", currentLocale).format(date))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(Long.parseLong(shutdownData.get("eventId").toString()))
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
