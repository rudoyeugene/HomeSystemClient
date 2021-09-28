package com.rudyii.hsw.client.services;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.LOG_ROOT;
import static com.rudyii.hsw.client.helpers.Utils.buildFromStringMap;
import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServers;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveStringValueToSettingsStorage;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getCustomRootReference;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rudyii.hs.common.objects.logs.CameraRebootLog;
import com.rudyii.hs.common.objects.logs.IspLog;
import com.rudyii.hs.common.objects.logs.LogBase;
import com.rudyii.hs.common.objects.logs.MotionLog;
import com.rudyii.hs.common.objects.logs.SimpleWatcherLog;
import com.rudyii.hs.common.objects.logs.StartStopLog;
import com.rudyii.hs.common.objects.logs.StateChangedLog;
import com.rudyii.hs.common.objects.logs.UploadLog;
import com.rudyii.hs.common.objects.message.FcmMessage;
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
    public static final String FCM_TOKEN = "FCM_TOKEN";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveStringValueToSettingsStorage(FCM_TOKEN, token);
        registerUserDataOnServers();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> objectProps = message.getData();
        FcmMessage fcmMessage = buildFromStringMap(objectProps, FcmMessage.class);
        String serverAlias = fcmMessage.getServerAlias();
        Long when = fcmMessage.getPublishedAt();

        getCustomRootReference(fcmMessage.getServerKey()).child(LOG_ROOT).child(fcmMessage.getPublishedAt() + "").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    LogBase logBase = snapshot.getValue(LogBase.class);

                    switch (logBase.getLogType()) {
                        case STATE_CHANGED:
                            StateChangedLog stateChangedLog = snapshot.getValue(StateChangedLog.class);
                            new StatusChangedNotifier(getApplicationContext(), stateChangedLog, serverAlias, when);
                            break;

                        case MOTION_DETECTED:
                            MotionLog motionLog = snapshot.getValue(MotionLog.class);
                            new MotionDetectedNotifier(getApplicationContext(), motionLog, serverAlias, when);
                            break;

                        case RECORD_UPLOADED:
                            UploadLog uploadLog = snapshot.getValue(UploadLog.class);
                            new VideoUploadedNotifier(getApplicationContext(), uploadLog, serverAlias, when);
                            break;

                        case ISP_CHANGED:
                            IspLog ispLog = snapshot.getValue(IspLog.class);
                            new WanInfoNotifier(getApplicationContext(), ispLog, serverAlias, when);
                            break;

                        case CAMERA_REBOOTED:
                            CameraRebootLog cameraRebootLog = snapshot.getValue(CameraRebootLog.class);
                            new CameraRebootNotifier(getApplicationContext(), cameraRebootLog, serverAlias, when);
                            break;

                        case SIMPLE_WATCHER_FIRED:
                            SimpleWatcherLog simpleWatcherLog = snapshot.getValue(SimpleWatcherLog.class);
                            new SimpleNotifier(getApplicationContext(), simpleWatcherLog, serverAlias, when);
                            break;

                        case SERVER_START_STOP:
                            StartStopLog startStopLog = snapshot.getValue(StartStopLog.class);
                            new ServerStartupShutdownNotifier(getApplicationContext(), startStopLog, serverAlias, when);
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}
