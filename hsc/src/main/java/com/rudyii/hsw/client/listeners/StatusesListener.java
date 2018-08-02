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

import java.util.HashMap;

import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static java.util.Objects.requireNonNull;

/**
 * Created by Jack on 17.12.2017.
 */

public class StatusesListener extends BroadcastReceiver {
    public static final String HSC_STATUSES_UPDATED = "com.rudyii.hsw.client.HSC_STATUSES_UPDATED";

    @Override
    public void onReceive(Context context, Intent intent) {
        @SuppressWarnings("unchecked") HashMap<String, Object> statusesData = (HashMap<String, Object>) intent.getSerializableExtra("HSC_STATUSES_UPDATED");
        String serverName = (String) statusesData.get("serverName");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(serverName + ": " + context.getResources().getString(R.string.notif_text_system_state_changed))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getResources().getString(R.string.notif_text_system_state_is)
                                + statusesData.get("systemModeText")
                                + ":" + statusesData.get("systemStateText")
                                + ", " + ((boolean) statusesData.get("portsState") ? context.getResources().getString(R.string.notif_text_ports_open_text) : context.getResources().getString(R.string.notif_text_ports_closed_text))))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500})
                .setSound(Uri.parse(getStringValueFromSettings("INFO_SOUND")), AudioManager.STREAM_NOTIFICATION);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        requireNonNull(mNotificationManager).notify((int) System.currentTimeMillis(), mBuilder.build());
    }
}
