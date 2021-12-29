package com.rudyii.hsw.client.notifiers;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_NORMAL;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getServerStateTypeLocalized;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.rudyii.hs.common.objects.logs.StartStopLog;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

/**
 * Created by Jack on 19.12.2017.
 */

public class ServerStartupShutdownNotifier {
    public ServerStartupShutdownNotifier(Context context, StartStopLog startStopLog, String serverAlias, long when) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NORMAL)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(String.format(currentLocale, "%s: %s",
                        serverAlias,
                        getServerStateTypeLocalized(startStopLog.getServerState())))
                .setContentText(String.format(currentLocale, "%s",
                        getCurrentTimeAndDateDoubleDotsDelimFrom(when)))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setWhen(when)
                .setVibrate(new long[]{0, 500});

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
