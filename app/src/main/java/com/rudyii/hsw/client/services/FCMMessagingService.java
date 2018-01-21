package com.rudyii.hsw.client.services;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.FirebaseListenersFactory.buildMotionRefValueEventListener;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.getServerKeyFromAlias;
import static com.rudyii.hsw.client.listeners.OfflineDeviceListener.HSC_DEVICE_REBOOT;
import static com.rudyii.hsw.client.listeners.ServerShutdownListener.HSC_SERVER_STOPPED;
import static com.rudyii.hsw.client.listeners.ServerStartupListener.HSC_SERVER_STARTED;
import static com.rudyii.hsw.client.listeners.StatusesListener.HSC_STATUSES_UPDATED;
import static com.rudyii.hsw.client.listeners.WanInfoListener.HSC_WAN_IP_CHANGED;

/**
 * Created by j-a-c on 26.12.2017.
 */

public class FCMMessagingService extends FirebaseMessagingService {
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.i(TAG, "Received new message with data" + message.getData().toString());
        Map<String, String> messageData = message.getData();
        HashMap<String, Object> extraData = new HashMap<>();
        String serverName = messageData.get("serverName");
        extraData.put("serverName", serverName);

        Intent intent = new Intent();

        switch (message.getData().get("reason")) {
            case "systemStateChanged":
                String armedMode = messageData.get("armedMode");
                String armedState = messageData.get("armedState");
                Boolean portsOpen = Boolean.valueOf(messageData.get("portsOpen"));

                intent.setAction(HSC_STATUSES_UPDATED);
                extraData.putAll(buildDataForMainActivityFrom(armedMode, armedState, portsOpen));
                intent.putExtra("HSC_STATUSES_UPDATED", extraData);
                getAppContext().sendBroadcast(intent);
                break;

            case "motionDetected":
                Long motionId = Long.valueOf(messageData.get("motionId"));
                DatabaseReference motionsRef = firebaseDatabase.getReference(getServerKeyFromAlias(serverName) + "/log/" + motionId);
                motionsRef.addListenerForSingleValueEvent(buildMotionRefValueEventListener(serverName));
                break;

            case "ispChanged":
                String currentIsp = messageData.get("isp");
                String currentWanIp = messageData.get("ip");

                extraData.put("wanIp", currentWanIp);
                extraData.put("isp", currentIsp);

                intent = new Intent();
                intent.setAction(HSC_WAN_IP_CHANGED);
                intent.putExtra("HSC_WAN_IP_CHANGED", extraData);
                getAppContext().sendBroadcast(intent);
                break;

            case "cameraReboot":
                String cameraInReboot = messageData.get("cameraName");

                extraData.put("cameraName", cameraInReboot);

                intent.setAction(HSC_DEVICE_REBOOT);
                intent.putExtra("HSC_DEVICE_REBOOT", extraData);
                getAppContext().sendBroadcast(intent);
                break;

            case "serverStartupOrShutdown":
                String action = messageData.get("action");

                switch (action) {
                    case "starting":
                        String serverPid = messageData.get("pid");
                        extraData.put("serverPid", serverPid);
                        intent.setAction(HSC_SERVER_STARTED);
                        intent.putExtra("HSC_SERVER_STARTED", extraData);
                        break;

                    case "stopping":
                        intent.setAction(HSC_SERVER_STOPPED);
                        intent.putExtra("HSC_SERVER_STOPPED", extraData);
                        break;

                    default:
                        Log.e(TAG, "Something wrong is happening with the server, please check urgently!");
                        break;
                }

                getAppContext().sendBroadcast(intent);
                break;

            default:
                Log.e(TAG, "Failed to process message with data: " + message.getData());
        }
    }
}
