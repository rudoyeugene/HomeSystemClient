package com.rudyii.hsw.client.services;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rudyii.hsw.client.helpers.FirebaseListenersFactory;

import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.getServerKey;
import static com.rudyii.hsw.client.listeners.OfflineDeviceListener.HSC_DEVICE_REBOOT;
import static com.rudyii.hsw.client.listeners.ServerShutdownListener.HSC_SERVER_STOPPED;
import static com.rudyii.hsw.client.listeners.ServerStartupListener.HSC_SERVER_STARTED;
import static com.rudyii.hsw.client.listeners.StatusesListener.HSC_STATUSES_UPDATED;
import static com.rudyii.hsw.client.listeners.WanInfoListener.HSC_WAN_IP_CHANGED;

/**
 * Created by j-a-c on 26.12.2017.
 */

public class FCMMessagingService extends FirebaseMessagingService {
    private FirebaseListenersFactory listenersFactory = FirebaseListenersFactory.getInstance();
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.i(TAG, "Received new message with data" + message.getData().toString());
        Map<String, String> messageData = message.getData();
        Intent intent = new Intent();

        switch (message.getData().get("reason")) {
            case "systemStateChanged":
                String armedMode = messageData.get("armedMode");
                String armedState = messageData.get("armedState");
                Boolean portsOpen = Boolean.valueOf(messageData.get("portsOpen"));

                intent.setAction(HSC_STATUSES_UPDATED);
                intent.putExtra("HSC_STATUSES_UPDATED", buildDataForMainActivityFrom(armedMode, armedState, portsOpen));
                getAppContext().sendBroadcast(intent);
                break;

            case "motionDetected":
                Long motionId = Long.valueOf(messageData.get("motionId"));
                DatabaseReference motionsRef = firebaseDatabase.getReference(getServerKey() + "/motions/" + motionId);
                motionsRef.addListenerForSingleValueEvent(listenersFactory.buildMotionRefValueEventListener());
                break;

            case "ispChanged":
                String currentIsp = messageData.get("isp");
                String currentWanIp = messageData.get("ip");

                HashMap<String, Object> wanInfoData = new HashMap<>();

                wanInfoData.put("wanIp", currentWanIp);
                wanInfoData.put("isp", currentIsp);

                intent = new Intent();
                intent.setAction(HSC_WAN_IP_CHANGED);
                intent.putExtra("HSC_WAN_IP_CHANGED", wanInfoData);
                getAppContext().sendBroadcast(intent);
                break;

            case "cameraReboot":
                String cameraInReboot = messageData.get("cameraName");
                intent.setAction(HSC_DEVICE_REBOOT);
                intent.putExtra("HSC_DEVICE_REBOOT", cameraInReboot);
                getAppContext().sendBroadcast(intent);
                break;

            case "serverStartupOrShutdown":
                String action = messageData.get("action");

                switch (action) {
                    case "starting":
                        String currentPid = messageData.get("pid");
                        intent.setAction(HSC_SERVER_STARTED);
                        intent.putExtra("HSC_SERVER_STARTED", currentPid);
                        break;

                    case "stopping":
                        intent.setAction(HSC_SERVER_STOPPED);
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
