package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_NORMAL;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.rudyii.hs.common.objects.logs.StateChangedLog;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

/**
 * Created by Jack on 17.12.2017.
 */

public class StatusChangedNotifier {
    public StatusChangedNotifier(Context context, StateChangedLog stateChangedLog, String serverAlias, Long when) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NORMAL)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(String.format(currentLocale, "%s: %s>%s",
                        serverAlias,
                        stateChangedLog.getSystemMode(),
                        stateChangedLog.getSystemState()))
                .setContentText(stateChangedLog.getBy())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(when)
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
