package com.rudyii.hsw.client;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.createNotificationChannels;
import static com.rudyii.hsw.client.helpers.ShortcutsBuilder.buildDynamicShortcuts;
import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServers;

/**
 * Created by Jack on 16.12.2017.
 */

public class HomeSystemClientApplication extends Application {
    public static final String TAG = "HSClient";
    private static Context appContext;
    private static String currentToken;

    public static void updateToken(String currentToken) {
        HomeSystemClientApplication.currentToken = currentToken;
    }

    public static String getToken() {
        return currentToken;
    }

    public static Context getAppContext() {
        return appContext;
    }

    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        FirebaseApp.initializeApp(getAppContext());

        Log.i(TAG, "HomeSystemClientApplication created");

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String newToken = task.getResult();
            updateToken(newToken);
            registerUserDataOnServers(getToken());
        });

        buildDynamicShortcuts();

        createNotificationChannels();
    }
}
