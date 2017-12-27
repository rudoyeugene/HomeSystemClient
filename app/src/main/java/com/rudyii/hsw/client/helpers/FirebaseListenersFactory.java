package com.rudyii.hsw.client.helpers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.client.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.buildMainActivityButtonsStateMapFrom;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateSingleDotDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.saveImageFromCamera;
import static com.rudyii.hsw.client.listeners.MotionListener.HSC_MOTION_DETECTED;
import static com.rudyii.hsw.client.listeners.OfflineDeviceListener.HSC_DEVICE_REBOOT;
import static com.rudyii.hsw.client.listeners.ServerStartupListener.HSC_SERVER_STARTED;
import static com.rudyii.hsw.client.listeners.StatusesListener.HSC_STATUSES_UPDATED;
import static com.rudyii.hsw.client.listeners.WanInfoListener.HSC_WAN_IP_CHANGED;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getLongValueFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveLongValueToSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveStringValueToSettings;

/**
 * Created by j-a-c on 27.12.2017.
 */

public class FirebaseListenersFactory {
    private static FirebaseListenersFactory instance;

    public static FirebaseListenersFactory getInstance() {
        if (instance == null) {
            instance = new FirebaseListenersFactory();
        }
        return instance;
    }

    public ValueEventListener buildStatusesValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> state = (Map<String, Object>) dataSnapshot.getValue();

                if (state == null) {
                    return;
                }

                Long currentTimeStamp = (long) state.get("timeStamp");
                Long savedTimeStamp = getLongValueFromSettings("STATUSES_TIME_STAMP");

                if (Objects.equals(currentTimeStamp, savedTimeStamp)) {
                    return;
                } else {
                    saveLongValueToSettings("STATUSES_TIME_STAMP", currentTimeStamp);
                }

                String armedMode = state.get("armedMode").toString();
                String armedState = state.get("armedState").toString();
                Boolean portsOpen = Boolean.valueOf(state.get("portsOpen").toString());

                HashMap<String, Object> statusesData = buildMainActivityButtonsStateMapFrom(armedMode, armedState);

                statusesData.put("systemModeText", armedMode);

                if (armedMode.equalsIgnoreCase("auto")) {
                    statusesData.put("systemModeTextColor", ContextCompat.getColor(getAppContext(), R.color.red));
                } else {
                    statusesData.put("systemModeTextColor", ContextCompat.getColor(getAppContext(), R.color.green));
                }

                statusesData.put("systemStateText", armedState);

                if (armedState.equalsIgnoreCase("armed")) {
                    statusesData.put("systemStateTextColor", ContextCompat.getColor(getAppContext(), R.color.red));
                } else if (armedState.equalsIgnoreCase("disarmed")) {
                    statusesData.put("systemStateTextColor", ContextCompat.getColor(getAppContext(), R.color.green));
                } else {
                    statusesData.put("systemStateTextColor", ContextCompat.getColor(getAppContext(), R.color.blue));
                }

                statusesData.put("portsState", portsOpen);

                Intent intent = new Intent();
                intent.setAction(HSC_STATUSES_UPDATED);
                intent.putExtra("HSC_STATUSES_UPDATED", statusesData);
                getAppContext().sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the statuses");
            }
        };
    }

    public ValueEventListener buildMotionRefValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> motion = (Map<String, Object>) dataSnapshot.getValue();

                if (motion == null) {
                    return;
                }

                String cameraName = dataSnapshot.getKey();
                Long currentTimeStamp = (long) motion.get("timeStamp");
                Long savedTimeStamp = getLongValueFromSettings(cameraName + "_MOTION_TIME_STAMP");

                if (Objects.equals(currentTimeStamp, savedTimeStamp)) {
                    return;
                } else {
                    saveLongValueToSettings(cameraName + "_MOTION_TIME_STAMP", currentTimeStamp);
                }

                HashMap<String, Object> motionData = new HashMap<>();
                motionData.put("cameraName", dataSnapshot.getKey());
                motionData.put("timeStamp", motion.get("timeStamp"));
                motionData.put("motionArea", motion.get("motionArea"));

                String imageString = (String) motion.get("image");
                byte[] decodedImageString = Base64.decode(imageString, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImageString, 0, decodedImageString.length);

                saveImageFromCamera(bitmap, motionData.get("cameraName").toString(), getCurrentTimeAndDateSingleDotDelimFrom(currentTimeStamp).toString());

                while (bitmap.getByteCount() > 512000) {
                    int srcWidth = bitmap.getWidth();
                    int srcHeight = bitmap.getHeight();
                    int dstWidth = (int) (srcWidth * 0.9f);
                    int dstHeight = (int) (srcHeight * 0.9f);
                    bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true);
                }

                motionData.put("image", bitmap);

                Intent intent = new Intent();
                intent.setAction(HSC_MOTION_DETECTED);
                intent.putExtra("HSC_MOTION_DETECTED", motionData);
                getAppContext().sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the camera");
            }
        };
    }

    public ValueEventListener buildWanInfoValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> motion = (Map<String, Object>) dataSnapshot.getValue();

                if (motion == null) {
                    return;
                }

                String currentWanIp = (String) motion.get("wanIp");
                String savedWanIp = getStringValueFromSettings("WAN_IP");

                if (Objects.equals(currentWanIp, savedWanIp)) {
                    return;
                } else {
                    saveStringValueToSettings("WAN_IP", currentWanIp);
                }

                String currentIsp = (String) motion.get("isp");
                HashMap<String, Object> wanInfoData = new HashMap<>();

                wanInfoData.put("wanIp", currentWanIp);
                wanInfoData.put("isp", currentIsp);

                Intent intent = new Intent();
                intent.setAction(HSC_WAN_IP_CHANGED);
                intent.putExtra("HSC_WAN_IP_CHANGED", wanInfoData);
                getAppContext().sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the WAN info");
            }
        };
    }

    public ValueEventListener buildOfflineDevicesValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> offlineDevice = (Map<String, Object>) dataSnapshot.getValue();

                if (offlineDevice == null) {
                    return;
                }

                for (String key : offlineDevice.keySet()) {
                    Long currentDevice = (long) offlineDevice.get(key);

                    if (currentDevice == 0) {
                        return;
                    }

                    Long savedDevice = getLongValueFromSettings("OFFLINE_DEVICE");

                    if (Objects.equals(currentDevice, savedDevice)) {
                        return;
                    } else {
                        saveLongValueToSettings("OFFLINE_DEVICE", currentDevice);

                        Intent intent = new Intent();
                        intent.setAction(HSC_DEVICE_REBOOT);
                        intent.putExtra("HSC_DEVICE_REBOOT", key);
                        getAppContext().sendBroadcast(intent);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the offline devices");
            }
        };
    }

    public ValueEventListener buildPidValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    return;
                }

                Long currentPid = (long) dataSnapshot.getValue();

                if (currentPid == 0) {
                    return;
                }

                Long savedPid = getLongValueFromSettings("SERVER_PID");

                if (Objects.equals(currentPid, savedPid)) {
                    return;
                } else {
                    saveLongValueToSettings("SERVER_PID", currentPid);

                    Intent intent = new Intent();
                    intent.setAction(HSC_SERVER_STARTED);
                    intent.putExtra("HSC_SERVER_STARTED", currentPid);
                    getAppContext().sendBroadcast(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the offline devices");
            }
        };
    }
}
