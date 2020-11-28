package com.rudyii.hsw.client.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rudyii.hsw.client.notifiers.CameraRebootNotifier;
import com.rudyii.hsw.client.notifiers.MotionDetectedNotifier;
import com.rudyii.hsw.client.notifiers.ServerShutdownNotifier;
import com.rudyii.hsw.client.notifiers.ServerStartupNotifier;
import com.rudyii.hsw.client.notifiers.SimpleNotifier;
import com.rudyii.hsw.client.notifiers.StatusChangedNotifier;
import com.rudyii.hsw.client.notifiers.VideoUploadedNotifier;
import com.rudyii.hsw.client.notifiers.WanInfoNotifier;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.updateToken;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServers;

/**
 * Created by Jack on 26.12.2017.
 */

public class FCMMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateToken(token);
        registerUserDataOnServers(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> messageData = message.getData();
        HashMap<String, Object> data = new HashMap<>(messageData);

        Date date = new Date(Long.parseLong(data.get("eventId").toString()));
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", currentLocale);
        dateFormat.setTimeZone(TimeZone.getDefault());
        data.put("eventDateTime", dateFormat.format(date));

        switch (data.get("reason").toString()) {
            case "systemStateChanged":
                String armedMode = messageData.get("armedMode");
                String armedState = messageData.get("armedState");

                data.putAll(buildDataForMainActivityFrom(armedMode, armedState));

                new StatusChangedNotifier(getApplicationContext(), data);
                break;

            case "motionDetected":
                new MotionDetectedNotifier(getApplicationContext(), data);
                break;

            case "videoRecorded":
                new VideoUploadedNotifier(getApplicationContext(), data);
                break;

            case "ispChanged":
                new WanInfoNotifier(getApplicationContext(), data);
                break;

            case "cameraReboot":
                new CameraRebootNotifier(getApplicationContext(), data);
                break;

            case "simpleNotification":
                new SimpleNotifier(getApplicationContext(), data);
                break;

            case "serverStateChanged":
                switch (data.get("action").toString()) {
                    case "started":
                        new ServerStartupNotifier(getApplicationContext(), data);
                        break;

                    case "stopped":
                        new ServerShutdownNotifier(getApplicationContext(), data);
                        break;

                    default:
                        Log.e(TAG, "Something wrong is happening with the server, please check urgently!");
                        break;
                }
                break;

            default:
                Log.e(TAG, "Failed to process message with data: " + message.getData());
        }
    }
}
