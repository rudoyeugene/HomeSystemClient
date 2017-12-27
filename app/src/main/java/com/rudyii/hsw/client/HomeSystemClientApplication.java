package com.rudyii.hsw.client;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.rudyii.hsw.client.services.FCMMessagingService;
import com.rudyii.hsw.client.services.FCMService;

import static com.rudyii.hsw.client.helpers.Utils.getSimplifiedPrimaryAccountName;
import static com.rudyii.hsw.client.helpers.Utils.isMyServiceRunning;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;

/**
 * Created by j-a-c on 16.12.2017.
 */

public class HomeSystemClientApplication extends Application {
    public static String TAG = "HSClient";
    private static Context appContext;

    public static Context getAppContext() {
        return appContext;
    }

    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "HomeSystemClientApplication created");

        appContext = getApplicationContext();

        if (!isMyServiceRunning(FCMMessagingService.class)) {
            startService(new Intent(getApplicationContext(), FCMMessagingService.class));
        }

        if (!isMyServiceRunning(FCMService.class)) {
            startService(new Intent(getApplicationContext(), FCMService.class));
        }

        String token = FirebaseInstanceId.getInstance().getToken();
        String accountName = getSimplifiedPrimaryAccountName();

        if (token != null && !accountName.equals("")) {
            getRootReference().child("/connectedClients/" + accountName).setValue(token);
        }
    }
}
