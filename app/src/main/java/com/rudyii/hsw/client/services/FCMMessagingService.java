package com.rudyii.hsw.client.services;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rudyii.hsw.client.helpers.FirebaseListenersFactory;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.getServerKey;

/**
 * Created by j-a-c on 26.12.2017.
 */

public class FCMMessagingService extends FirebaseMessagingService {
    private FirebaseListenersFactory listenersFactory = FirebaseListenersFactory.getInstance();
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.i(TAG, "Received new message with data" + message.getData().toString());

        switch (message.getData().get("reason")) {
            case "systemStateChanged":
                DatabaseReference statusesRef = firebaseDatabase.getReference(getServerKey() + "/statuses");
                statusesRef.addListenerForSingleValueEvent(listenersFactory.buildStatusesValueEventListener());
                break;

            case "motionDetected":
                DatabaseReference motionsRef = firebaseDatabase.getReference(getServerKey() + "/motions/" + message.getData().get("cameraName"));
                motionsRef.addListenerForSingleValueEvent(listenersFactory.buildMotionRefValueEventListener());
                break;

            case "ispChanged":
                DatabaseReference wanInfoRef = firebaseDatabase.getReference(getServerKey() + "/info/wanInfo");
                wanInfoRef.addListenerForSingleValueEvent(listenersFactory.buildWanInfoValueEventListener());
                break;

            case "cameraReboot":
                DatabaseReference offlineDevicesRef = firebaseDatabase.getReference(getServerKey() + "/offlineDevices");
                offlineDevicesRef.addListenerForSingleValueEvent(listenersFactory.buildOfflineDevicesValueEventListener());
                break;

            case "serverStarted":
                DatabaseReference pidRef = firebaseDatabase.getReference(getServerKey() + "/info/pid");
                pidRef.addListenerForSingleValueEvent(listenersFactory.buildPidValueEventListener());
                break;

            default:
                Log.e(TAG, "Failed to process message with data: " + message.getData());
        }
    }
}
