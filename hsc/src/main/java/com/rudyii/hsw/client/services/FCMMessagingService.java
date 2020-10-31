package com.rudyii.hsw.client.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.updateToken;
import static com.rudyii.hsw.client.helpers.FirebaseListenersFactory.buildRecordRefValueEventListener;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.getServerKeyFromAlias;
import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServers;
import static com.rudyii.hsw.client.notifiers.MotionDetectedNotifier.notifyAboutMotionDetected;
import static com.rudyii.hsw.client.notifiers.OfflineDeviceReceiver.notifyAboutDeviceGoneOffline;
import static com.rudyii.hsw.client.notifiers.ServerShutdownNotifier.notifyAboutServerStopped;
import static com.rudyii.hsw.client.notifiers.ServerStartupNotifier.notifyAboutServerStarted;
import static com.rudyii.hsw.client.notifiers.StatusChangedReceiver.notifyAboutSystemStateChanged;
import static com.rudyii.hsw.client.notifiers.WanInfoReceiver.notifyAboutWanChanges;

/**
 * Created by Jack on 26.12.2017.
 */

public class FCMMessagingService extends FirebaseMessagingService {
    private final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

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
        String serverName = messageData.get("serverName");

        switch (message.getData().get("reason")) {
            case "systemStateChanged":
                String armedMode = messageData.get("armedMode");
                String armedState = messageData.get("armedState");

                data.putAll(buildDataForMainActivityFrom(armedMode, armedState));

                notifyAboutSystemStateChanged(data);
                break;

            case "motionDetected":
                Date date = new Date(Long.parseLong(messageData.get("eventId")));
                DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy_HH-mm-ss", currentLocale);
                dateFormat.setTimeZone(TimeZone.getDefault());
                notifyAboutMotionDetected(messageData);
                break;

            case "videoRecorded":
                Long recordId = Long.valueOf(messageData.get("recordId"));
                DatabaseReference recordsRef = firebaseDatabase.getReference(getServerKeyFromAlias(serverName) + "/log/" + recordId);
                recordsRef.addListenerForSingleValueEvent(buildRecordRefValueEventListener(serverName));
                break;

            case "ispChanged":
                String currentIsp = messageData.get("isp");
                String currentWanIp = messageData.get("ip");

                data.put("wanIp", currentWanIp);
                data.put("isp", currentIsp);

                notifyAboutWanChanges(data);
                break;

            case "cameraReboot":
                String cameraInReboot = messageData.get("cameraName");

                data.put("cameraName", cameraInReboot);

                notifyAboutDeviceGoneOffline(data);
                break;

            case "serverStartupOrShutdown":
                String action = messageData.get("action");

                switch (action) {
                    case "starting":
                        String serverPid = messageData.get("pid");
                        data.put("serverPid", serverPid);
                        notifyAboutServerStarted(data);
                        break;

                    case "stopping":
                        notifyAboutServerStopped(data);
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
