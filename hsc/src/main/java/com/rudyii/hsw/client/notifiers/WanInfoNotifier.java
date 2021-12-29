package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_HIGH;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.rudyii.hs.common.objects.logs.IspLog;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

/**
 * Created by Jack on 18.12.2017.
 */

public class WanInfoNotifier {
    public WanInfoNotifier(Context context, IspLog ispLog, String serverAlias, long when) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(String.format(currentLocale, "%s %s",
                        serverAlias,
                        context.getResources().getString(R.string.notif_text_isp_changed)))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(String.format(currentLocale, "%s: %s\n%s: %s",
                                context.getResources().getString(R.string.notif_text_current_isp),
                                ispLog.getIspName(),
                                context.getResources().getString(R.string.notif_text_current_ip),
                                ispLog.getIspIp())))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(when)
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
