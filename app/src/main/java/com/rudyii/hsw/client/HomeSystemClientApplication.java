package com.rudyii.hsw.client;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import static com.rudyii.hsw.client.helpers.Utils.getSimplifiedPrimaryAccountName;
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

        String token = FirebaseInstanceId.getInstance().getToken();
        String accountName = getSimplifiedPrimaryAccountName();

        if (token != null && !"".equals(accountName)) {
            getRootReference().child("/connectedClients/" + accountName).setValue(token);
        }
    }
}
