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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class SimpleNotifier {
    public SimpleNotifier(Context context, HashMap<String, Object> simpleData) {
        String serverName = (String) simpleData.get("serverName");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(context.getResources().getString(R.string.notif_text_simple_event) + serverName)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(new String(Base64.getDecoder().decode(simpleData.get("simpleWatcherNotificationTextEncoded").toString()), StandardCharsets.UTF_8)))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(Long.parseLong(simpleData.get("eventId").toString()))
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
