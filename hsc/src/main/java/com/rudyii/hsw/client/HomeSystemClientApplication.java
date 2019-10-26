package com.rudyii.hsw.client;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import io.fabric.sdk.android.Fabric;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.N_MR1;
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
        Fabric.with(this, new Crashlytics());
        FirebaseApp.initializeApp(getAppContext());

        Log.i(TAG, "HomeSystemClientApplication created");

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                updateToken(newToken);
                registerUserDataOnServers(getToken());
            }
        });

        if (SDK_INT > N) {
            buildDynamicShortcuts();
        }

        if (SDK_INT > N_MR1) {
            createNotificationChannels();
        }
    }
}
