package com.rudyii.hsw.client.services;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.FirebaseListenersFactory.buildMotionRefValueEventListener;
import static com.rudyii.hsw.client.helpers.FirebaseListenersFactory.buildRecordRefValueEventListener;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.getServerKeyFromAlias;
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
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> messageData = message.getData();
        HashMap<String, Object> data = new HashMap<>();
        String serverName = messageData.get("serverName");
        data.put("serverName", serverName);

        switch (message.getData().get("reason")) {
            case "systemStateChanged":
                String armedMode = messageData.get("armedMode");
                String armedState = messageData.get("armedState");

                data.putAll(buildDataForMainActivityFrom(armedMode, armedState));

                notifyAboutSystemStateChanged(data);
                break;

            case "motionDetected":
                Long motionId = Long.valueOf(messageData.get("motionId"));
                DatabaseReference motionsRef = firebaseDatabase.getReference(getServerKeyFromAlias(serverName) + "/log/" + motionId);
                motionsRef.addListenerForSingleValueEvent(buildMotionRefValueEventListener(serverName));
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
