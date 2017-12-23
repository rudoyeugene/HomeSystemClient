package com.rudyii.hsw.client.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.client.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.buildMainActivityButtonsStateMapFrom;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateSingleDotDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getServerKey;
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
 * Created by j-a-c on 11.12.2017.
 */

public class FirebaseService extends Service {
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private List<ValueEventListener> valueEventListeners = new ArrayList<>();
    private List<DatabaseReference> databaseReferences = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting Firebase Service");

        if (getServerKey().equals("")) {
            return START_NOT_STICKY;
        }

        //Read settings once
        DatabaseReference settingsRef = firebaseDatabase.getReference(getServerKey() + "/settings");
        settingsRef.addListenerForSingleValueEvent(buildSettingsRefValueEventListener());

        //Subscribe on camera motions
        String cameraList = getStringValueFromSettings("CAMERA_LIST");

        List<String> cameras = Arrays.asList(cameraList.split(","));

        if (cameraAvailable(cameras)) {
            for (String cameraName : cameras) {
                DatabaseReference motionsRef = firebaseDatabase.getReference(getServerKey() + "/motions/" + cameraName);
                databaseReferences.add(motionsRef);

                ValueEventListener motionRefValueEventListener = buildMotionRefValueEventListener();
                motionsRef.addValueEventListener(motionRefValueEventListener);
                valueEventListeners.add(motionRefValueEventListener);
            }
        }

        //Subscribe to statuses
        DatabaseReference statusesRef = firebaseDatabase.getReference(getServerKey() + "/statuses");
        databaseReferences.add(statusesRef);
        ValueEventListener statusesValueEventListener = buildStatusesValueEventListener();
        statusesRef.addValueEventListener(statusesValueEventListener);
        valueEventListeners.add(statusesValueEventListener);

        //Subscribe to wanInfo
        DatabaseReference wanInfoRef = firebaseDatabase.getReference(getServerKey() + "/info/wanInfo");
        databaseReferences.add(wanInfoRef);
        ValueEventListener wanInfoValueEventListener = buildWanInfoValueEventListener();
        wanInfoRef.addValueEventListener(wanInfoValueEventListener);
        valueEventListeners.add(wanInfoValueEventListener);

        //Subscribe to offline devices
        DatabaseReference offlineDevicesRef = firebaseDatabase.getReference(getServerKey() + "/offlineDevices");
        databaseReferences.add(offlineDevicesRef);
        ValueEventListener offlineDevicesValueEventListener = buildOfflineDevicesValueEventListener();
        offlineDevicesRef.addValueEventListener(offlineDevicesValueEventListener);
        valueEventListeners.add(offlineDevicesValueEventListener);

        //Subscribe to pid
        DatabaseReference pidRef = firebaseDatabase.getReference(getServerKey() + "/info/pid");
        databaseReferences.add(pidRef);
        ValueEventListener pidValueEventListener = buildPidValueEventListener();
        pidRef.addValueEventListener(pidValueEventListener);
        valueEventListeners.add(pidValueEventListener);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping Firebase Service");

        for (DatabaseReference reference : databaseReferences) {
            for (ValueEventListener listener : valueEventListeners) {
                reference.removeEventListener(listener);
            }
        }

        databaseReferences.clear();
        valueEventListeners.clear();
    }

    private ValueEventListener buildStatusesValueEventListener() {
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
                sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the statuses");
            }
        };
    }

    private ValueEventListener buildMotionRefValueEventListener() {
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

                saveImage(bitmap, motionData.get("cameraName").toString(), getCurrentTimeAndDateSingleDotDelimFrom(currentTimeStamp).toString());

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
                sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the camera");
            }
        };
    }

    private ValueEventListener buildWanInfoValueEventListener() {
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
                sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the WAN info");
            }
        };
    }

    private ValueEventListener buildOfflineDevicesValueEventListener() {
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
                        sendBroadcast(intent);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the offline devices");
            }
        };
    }

    private ValueEventListener buildPidValueEventListener() {
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
                    sendBroadcast(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to subscribe to the offline devices");
            }
        };
    }

    private ValueEventListener buildSettingsRefValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> settings = (Map<String, Object>) dataSnapshot.getValue();

                if (settings == null) {
                    return;
                }

                String cameraList = (String) settings.get("cameraList");
                saveStringValueToSettings("CAMERA_LIST", cameraList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch settings");
            }
        };
    }

    public void saveImage(Bitmap bitmap, String cameraName, String imageName) {
        imageName = imageName + ".png";

        FileOutputStream fos = null;

        try {
            final File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/HomeSystemMotions/" + cameraName + "/");

            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "could not create the directories");
                }
            }

            final File motionImage = new File(directory, imageName);

            if (!motionImage.exists()) {
                motionImage.createNewFile();
            }

            fos = new FileOutputStream(motionImage);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = Uri.fromFile(motionImage);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(intent);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean cameraAvailable(List<String> cameras) {
        return !cameras.get(0).equals("");
    }
}
