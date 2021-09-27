package com.rudyii.hsw.client.services;

import static com.rudyii.hsw.client.HomeSystemClientApplication.updateToken;
import static com.rudyii.hsw.client.helpers.Utils.buildFromStringMap;
import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServers;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rudyii.hs.common.objects.message.CameraRebootMessage;
import com.rudyii.hs.common.objects.message.IspChangedMessage;
import com.rudyii.hs.common.objects.message.MessageBase;
import com.rudyii.hs.common.objects.message.MotionDetectedMessage;
import com.rudyii.hs.common.objects.message.ServerStartedStoppedMessage;
import com.rudyii.hs.common.objects.message.SimpleWatchMessage;
import com.rudyii.hs.common.objects.message.StateChangedMessage;
import com.rudyii.hs.common.objects.message.VideoUploadedMessage;
import com.rudyii.hsw.client.notifiers.CameraRebootNotifier;
import com.rudyii.hsw.client.notifiers.MotionDetectedNotifier;
import com.rudyii.hsw.client.notifiers.ServerStartupShutdownNotifier;
import com.rudyii.hsw.client.notifiers.SimpleNotifier;
import com.rudyii.hsw.client.notifiers.StatusChangedNotifier;
import com.rudyii.hsw.client.notifiers.VideoUploadedNotifier;
import com.rudyii.hsw.client.notifiers.WanInfoNotifier;

import java.util.Map;

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
        Map<String, String> objectProps = message.getData();
        MessageBase messageBase = buildFromStringMap(objectProps, MessageBase.class);

        switch (messageBase.getMessageType()) {
            case STATE_CHANGED:
                StateChangedMessage stateChangedMessage = buildFromStringMap(objectProps, StateChangedMessage.class);
                new StatusChangedNotifier(getApplicationContext(), stateChangedMessage);
                break;

            case MOTION_DETECTED:
                MotionDetectedMessage motionDetectedMessage = buildFromStringMap(objectProps, MotionDetectedMessage.class);
                new MotionDetectedNotifier(getApplicationContext(), motionDetectedMessage);
                break;

            case RECORD_UPLOADED:
                VideoUploadedMessage videoUploadedMessage = buildFromStringMap(objectProps, VideoUploadedMessage.class);
                new VideoUploadedNotifier(getApplicationContext(), videoUploadedMessage);
                break;

            case ISP_CHANGED:
                IspChangedMessage ispChangedMessage = buildFromStringMap(objectProps, IspChangedMessage.class);
                new WanInfoNotifier(getApplicationContext(), ispChangedMessage);
                break;

            case CAMERA_REBOOTED:
                CameraRebootMessage cameraRebootMessage = buildFromStringMap(objectProps, CameraRebootMessage.class);
                new CameraRebootNotifier(getApplicationContext(), cameraRebootMessage);
                break;

            case SIMPLE_WATCHER_FIRED:
                SimpleWatchMessage simpleWatchMessage = buildFromStringMap(objectProps, SimpleWatchMessage.class);
                new SimpleNotifier(getApplicationContext(), simpleWatchMessage);
                break;

            case SERVER_START_STOP:
                ServerStartedStoppedMessage serverStartedStoppedMessage = buildFromStringMap(objectProps, ServerStartedStoppedMessage.class);
                new ServerStartupShutdownNotifier(getApplicationContext(), serverStartedStoppedMessage);
                break;
        }
    }
}
