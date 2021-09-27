package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_HIGH;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.rudyii.hs.common.objects.message.SimpleWatchMessage;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SimpleNotifier {
    public SimpleNotifier(Context context, SimpleWatchMessage simpleWatchMessage) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(String.format(currentLocale, "%s: %s",
                        context.getResources().getString(R.string.notif_text_simple_event),
                        simpleWatchMessage.getServerAlias()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(new String(Base64.getDecoder().decode(simpleWatchMessage.getEncodedText()), StandardCharsets.UTF_8)))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(simpleWatchMessage.getPublishedAt())
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
