package com.rudyii.hsw.client;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import static com.rudyii.hsw.client.helpers.Utils.getCurrentFcmToken;
import static com.rudyii.hsw.client.helpers.Utils.registerTokenOnServers;

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

        registerTokenOnServers(getCurrentFcmToken());
    }
}
