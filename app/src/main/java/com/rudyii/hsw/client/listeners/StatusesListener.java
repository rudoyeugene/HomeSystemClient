package com.rudyii.hsw.client.listeners;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.activities.MainActivity;

import java.util.HashMap;

import static android.media.AudioManager.STREAM_NOTIFICATION;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;

/**
 * Created by j-a-c on 17.12.2017.
 */

public class StatusesListener extends BroadcastReceiver {
    public static String HSC_STATUSES_UPDATED = "com.rudyii.hsw.client.HSC_STATUSES_UPDATED";

    @Override
    public void onReceive(Context context, Intent intent) {
        HashMap<String, Object> statusesData = (HashMap<String, Object>) intent.getSerializableExtra("HSC_STATUSES_UPDATED");

        if (statusesData == null) {
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("System state changed")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("System state is "
                                + statusesData.get("systemModeText")
                                + ":" + statusesData.get("systemStateText")
                                + ", ports are " + ((boolean) statusesData.get("portsState") ? "open" : "closed")))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            Uri soundUri = Uri.parse(getStringValueFromSettings("INFO_SOUND"));
            Ringtone r = RingtoneManager.getRingtone(getAppContext(), soundUri);
            r.setStreamType(STREAM_NOTIFICATION);
            r.play();
        } catch (Exception e) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getAppContext(), soundUri);
            r.setStreamType(STREAM_NOTIFICATION);
            r.play();
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(333, mBuilder.build());
    }
}
