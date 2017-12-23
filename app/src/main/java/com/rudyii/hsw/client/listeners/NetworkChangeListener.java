package com.rudyii.hsw.client.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.rudyii.hsw.client.services.FirebaseService;

import static com.rudyii.hsw.client.helpers.Utils.isMyServiceRunning;

/**
 * Created by j-a-c on 19.12.2017.
 */

public class NetworkChangeListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (isMyServiceRunning(FirebaseService.class)) {
            context.stopService(new Intent(context, FirebaseService.class));
        }

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            if (!isMyServiceRunning(FirebaseService.class)) {
                context.startService(new Intent(context, FirebaseService.class));
            }
        }
    }
}
