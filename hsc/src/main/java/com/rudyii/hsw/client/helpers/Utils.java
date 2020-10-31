package com.rudyii.hsw.client.helpers;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.rudyii.hsw.client.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import static android.text.TextUtils.isDigitsOnly;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveStringValueToSettings;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getCustomReference;
import static java.util.Objects.requireNonNull;

/**
 * Created by Jack on 18.12.2017.
 */

@SuppressWarnings("WeakerAccess")
public class Utils {
    public static final String SERVER_LIST = "SERVER_LIST";
    public static final String DELAYED_ARM_DELAY_SECS = "DELAYED_ARM_DELAY_SECS";
    public static final String NOTIFICATION_TYPES = "NOTIFICATION_TYPES";
    public static final String NOTIFICATIONS_MUTED = "NOTIFICATIONS_MUTED";
    public static final String ACTIVE_SERVER = "ACTIVE_SERVER";
    public static final String INFO_SOUND = "INFO_SOUND";
    public static final String MOTION_SOUND = "MOTION_SOUND";
    public static final String CAMERA_APP = "CAMERA_APP";
    public static final String NOTIFICATION_TYPE_MOTION_DETECTED = "motionDetected";
    public static final String NOTIFICATION_TYPE_VIDEO_RECORDED = "videoRecorded";
    public static final String NOTIFICATION_TYPE_ALL = "all";
    public static final String NOTIFICATION_TYPE_MUTE = "mute";
    public static final Locale currentLocale = getAppContext().getResources().getConfiguration().locale;
    private static final String HOURLY_REPORT_STATE = "HOURLY_REPORT_STATE";

    public static String getCurrentTimeAndDateDoubleDotsDelimFrom(Long timeStamp) {
        if (timeStamp == null) {
            return "";
        }

        Date date = new Date(timeStamp);
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy", currentLocale);
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
    }

    public static String getCurrentTimeAndDateSingleDotDelimFrom(Long timeStamp) {
        if (timeStamp == null) {
            return "";
        }

        Date date = new Date(timeStamp);
        DateFormat dateFormat = new SimpleDateFormat("HH.mm.ss-dd.MM.yyyy", currentLocale);
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
    }

    public static String getSoundNameBy(String soundUri) {
        Ringtone ringtone = RingtoneManager.getRingtone(getAppContext(), Uri.parse(soundUri));
        return ringtone.getTitle(getAppContext());
    }

    public static boolean isPaired() {
        String activeServerAlias = getActiveServerAlias();
        String activeServerKey = getServerKeyFromAlias(activeServerAlias);
        return serverKeyIsValid(activeServerKey);
    }

    public static boolean serverKeyIsValid(String serverKey) {
        try {
            return null != UUID.fromString(serverKey);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getServerKeyFromAlias(String serverAlias) {
        return getMapWithServers().get(serverAlias);
    }

    public static HashMap<String, Object> buildDataForMainActivityFrom(String mode, String state) {
        HashMap<String, Object> result = new HashMap<>();
        switch (mode.toLowerCase()) {
            case "automatic":
                result.put("systemModeText", getAppContext().getResources().getString(R.string.toggle_button_text_system_mode_state_automatic).toUpperCase());
                break;
            case "manual":
                result.put("systemModeText", getAppContext().getResources().getString(R.string.toggle_button_text_system_mode_manual).toUpperCase());
                break;
            default:
                result.put("systemModeText", "UNKNOWN_MODE");
        }

        switch (state.toLowerCase()) {
            case "armed":
                result.put("systemStateText", getAppContext().getResources().getString(R.string.toggle_button_text_system_state_armed).toUpperCase());
                break;
            case "disarmed":
                result.put("systemStateText", getAppContext().getResources().getString(R.string.toggle_button_text_system_state_disarmed).toUpperCase());
                break;
            case "auto":
                result.put("systemStateText", getAppContext().getResources().getString(R.string.toggle_button_text_system_mode_state_automatic).toUpperCase());
                break;
            default:
                result.put("systemStateText", "UNKNOWN_STATE");
        }

        if ("auto".equalsIgnoreCase(mode)) {
            result.put("systemModeTextColor", ContextCompat.getColor(getAppContext(), R.color.red));
        } else {
            result.put("systemModeTextColor", ContextCompat.getColor(getAppContext(), R.color.green));
        }

        if ("armed".equalsIgnoreCase(state)) {
            result.put("systemStateTextColor", ContextCompat.getColor(getAppContext(), R.color.red));
        } else if (state.equalsIgnoreCase("disarmed")) {
            result.put("systemStateTextColor", ContextCompat.getColor(getAppContext(), R.color.green));
        } else {
            result.put("systemStateTextColor", ContextCompat.getColor(getAppContext(), R.color.blue));
        }

        if ("automatic".equalsIgnoreCase(mode) && "armed".equalsIgnoreCase(state)) {
            result.put("systemModeChecked", true);
            result.put("systemStateChecked", true);
            result.put("systemStateEnabled", false);
        } else if ("automatic".equalsIgnoreCase(mode) && ("disarmed".equalsIgnoreCase(state) || state.equalsIgnoreCase("auto"))) {
            result.put("systemModeChecked", true);
            result.put("systemStateChecked", false);
            result.put("systemStateEnabled", false);
        } else if (!"automatic".equalsIgnoreCase(mode) && "armed".equalsIgnoreCase(state)) {
            result.put("systemModeChecked", false);
            result.put("systemStateChecked", true);
            result.put("systemStateEnabled", true);
        } else if (!"automatic".equalsIgnoreCase(mode) && "disarmed".equalsIgnoreCase(state)) {
            result.put("systemModeChecked", false);
            result.put("systemStateChecked", false);
            result.put("systemStateEnabled", true);
        } else {
            result.put("systemModeChecked", true);
            result.put("systemStateChecked", true);
            result.put("systemStateEnabled", true);
        }

        return result;
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceId() {
        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) getAppContext().getSystemService(serviceName);
        String deviceId;

        if (ActivityCompat.checkSelfPermission(getAppContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            deviceId = Settings.Secure.getString(getAppContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        } else {
            deviceId = requireNonNull(m_telephonyManager).getDeviceId();
        }

        return deviceId;
    }

    public static void registerUserDataOnServers(String token) {
        if (!getMapWithServers().isEmpty()) {
            for (Map.Entry<String, String> entry : getMapWithServers().entrySet()) {
                String serverName = entry.getKey();
                String serverKey = entry.getValue();
                registerUserDataOnServer(serverKey, serverName, token);
            }
        }
    }

    public static void registerUserDataOnServer(String serverKey, String serverName, String token) {
        String simplifiedPrimaryAccountName = getSimplifiedPrimaryAccountName();
        String deviceId = getDeviceId();

        HashMap<String, Object> clientData = new HashMap<>();

        String notificationType = getNotificationTypeForServer(serverName);
        String hourlyReportMuted = getHourlyReportMutedStateForServer(serverName);
        String notificationsMuted = getNotificationMutedForServer(serverName);

        clientData.put("notificationType", notificationType);
        clientData.put("notificationsMuted", Boolean.parseBoolean(notificationsMuted));
        clientData.put("hourlyReportMuted", Boolean.parseBoolean(hourlyReportMuted));
        clientData.put("lastRegistration", System.currentTimeMillis());
        clientData.put("token", token);
        clientData.put("device", android.os.Build.MODEL);
        clientData.put("email", getPrimaryAccountEmail());

        PackageInfo pInfo;
        String version = "0.0.0";
        try {
            pInfo = getAppContext().getPackageManager().getPackageInfo(getAppContext().getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        clientData.put("appVersion", version);

        if (stringIsNotEmptyOrNull(simplifiedPrimaryAccountName)) {
            if (stringIsNotEmptyOrNull(deviceId)) {
                getCustomReference(serverKey).child("/connectedClients/" + deviceId).removeValue();
            }
            getCustomReference(serverKey).child("/connectedClients/" + simplifiedPrimaryAccountName).setValue(clientData);
        } else if (stringIsNotEmptyOrNull(deviceId)) {
            if (stringIsNotEmptyOrNull(simplifiedPrimaryAccountName)) {
                getCustomReference(serverKey).child("/connectedClients/" + simplifiedPrimaryAccountName).removeValue();
            }
            getCustomReference(serverKey).child("/connectedClients/" + deviceId).setValue(clientData);
        } else {
            new ToastDrawer().showToast(getAppContext().getResources().getString(R.string.toast_failed_to_register_on_server));
        }
    }

    public static String getHourlyReportMutedStateForServer(String serverName) {
        return stringIsEmptyOrNull(getHourlyReportMutedStates().get(serverName)) ? "false" : getHourlyReportMutedStates().get(serverName);
    }

    public static void saveHourlyReportMutedStateForServer(String serverName, String state) {
        HashMap<String, String> states = getHourlyReportMutedStates();
        states.put(serverName, state);
        saveMapToSettings(states, HOURLY_REPORT_STATE);
    }

    private static String getSimplifiedPrimaryAccountName() {
        Account[] accounts = getAccounts();
        String simplifiedAccountName = "";

        if (accounts.length > 0) {
            Account mainAccount = accounts[0];
            simplifiedAccountName = mainAccount.name.split("@")[0].replace(".", "");

        }

        return simplifiedAccountName;
    }

    private static String getPrimaryAccountEmail() {
        Account[] accounts = getAccounts();
        String primaryAccountEmail = "";

        if (accounts.length > 0) {
            primaryAccountEmail = accounts[0].name;
        }

        return primaryAccountEmail;
    }

    public static boolean stringIsEmptyOrNull(String string) {
        return string == null || "".equalsIgnoreCase(string);
    }

    public static boolean stringIsNotEmptyOrNull(String string) {
        return !stringIsEmptyOrNull(string);
    }

    public static void switchActiveServerTo(String serverAlias) {
        saveStringValueToSettings(ACTIVE_SERVER, serverAlias);
    }

    public static void removeServerFromServersList(String serverAlias) {
        HashMap<String, String> serverListMap = getMapWithServers();

        serverListMap.remove(serverAlias);
        saveMapToSettings(serverListMap, SERVER_LIST);
    }

    public static String getActiveServerAlias() {
        return getStringValueFromSettings(ACTIVE_SERVER);
    }

    public static String getActiveServerKey() {
        HashMap<String, String> availableServers = getMapWithServers();
        String activeServerAlias = getActiveServerAlias();
        return availableServers.get(activeServerAlias) == null ? "dummyServer" : availableServers.get(activeServerAlias);
    }

    public static ArrayList<String> getServersList() {
        ArrayList<String> serversList = new ArrayList<>();

        for (Map.Entry<String, String> entry : getMapWithServers().entrySet()) {
            serversList.add(entry.getKey());
        }
        return serversList;
    }

    private static HashMap<String, String> getMapWithServers() {
        return (HashMap<String, String>) getMapFromSettings(SERVER_LIST);
    }

    public static String getNotificationTypeForServer(String serverName) {
        return stringIsEmptyOrNull(getNotificationTypes().get(serverName)) ? NOTIFICATION_TYPE_MOTION_DETECTED : getNotificationTypes().get(serverName);
    }

    private static HashMap<String, String> getNotificationTypes() {
        return (HashMap<String, String>) getMapFromSettings(NOTIFICATION_TYPES);
    }

    private static HashMap<String, String> getHourlyReportMutedStates() {
        return (HashMap<String, String>) getMapFromSettings(HOURLY_REPORT_STATE);
    }

    private static HashMap<String, String> getNotificationMuted() {
        return (HashMap<String, String>) getMapFromSettings(NOTIFICATIONS_MUTED);
    }

    public static String getNotificationMutedForServer(String serverName) {
        return stringIsEmptyOrNull(getNotificationMuted().get(serverName)) ? "false" : getNotificationMuted().get(serverName);
    }

    public static void saveNotificationMutedForServer(String serverName, String muted) {
        HashMap<String, String> serversNotificationTypes = (HashMap<String, String>) getMapFromSettings(NOTIFICATIONS_MUTED);
        serversNotificationTypes.put(serverName, muted);
        saveMapToSettings(serversNotificationTypes, NOTIFICATIONS_MUTED);
    }

    public static void saveNotificationTypeForServer(String serverName, String notificationType) {
        HashMap<String, String> serversNotificationTypes = (HashMap<String, String>) getMapFromSettings(NOTIFICATION_TYPES);
        serversNotificationTypes.put(serverName, notificationType);
        saveMapToSettings(serversNotificationTypes, NOTIFICATION_TYPES);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getMapFromSettings(String id) {
        String mapJson = getStringValueFromSettings(id);
        Gson gson = new Gson();
        return gson.fromJson(mapJson, HashMap.class) == null ? new HashMap<>() : new HashMap<>(gson.fromJson(mapJson, HashMap.class));
    }

    public static void saveMapToSettings(Map<String, String> map, String id) {
        Gson gson = new Gson();
        saveStringValueToSettings(id, gson.toJson(map));
    }

    public static String getCurrentFcmToken() {
        FirebaseApp.initializeApp(getAppContext());
        return FirebaseInstanceId.getInstance().getToken();
    }

    public static String[] retrievePermissions() {
        try {
            return getAppContext()
                    .getPackageManager()
                    .getPackageInfo(getAppContext().getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("This should have never happened.", e);
        }
    }

    public static Looper getLooper() {
        HandlerThread thread = new HandlerThread("Thread: " + new Random().nextInt(1000));
        thread.start();
        return thread.getLooper();
    }

    public static void saveImageFromCamera(Bitmap bitmap, String serverName, String cameraName, String imageName) {
        imageName = imageName + ".jpg";

        FileOutputStream fos = null;

        try {
            final File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/HomeSystemMotions/" + serverName + "/" + cameraName + "/");

            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "could not create the directories");
                }
            }

            final File motionImage = new File(directory, imageName);

            if (!motionImage.exists()) {
                //noinspection ResultOfMethodCallIgnored
                motionImage.createNewFile();
            }

            fos = new FileOutputStream(motionImage);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            Uri uri = Uri.fromFile(motionImage);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            getAppContext().sendBroadcast(intent);

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

    public static Bitmap readImageFromUrl(String imageUrl) {
        Bitmap bitmap = null;

        try (InputStream iStream = new URL(imageUrl).openConnection().getInputStream()) {
            bitmap = BitmapFactory.decodeStream(iStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load file", e);
        }
        return bitmap;
    }

    public static void saveDataFromUrl(String dataUrl, File destination) {
        try (InputStream iStream = new URL(dataUrl).openConnection().getInputStream()) {
            BufferedInputStream inStream = new BufferedInputStream(iStream, 8192);
            FileOutputStream fos = new FileOutputStream(destination);
            int len;
            byte[] buff = new byte[8192];
            while ((len = inStream.read(buff)) != -1) {
                fos.write(buff, 0, len);
            }
            fos.flush();
            fos.close();
            inStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to load file", e);
        }
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @NonNull
    private static Account[] getAccounts() {
        return AccountManager.get(getAppContext()).getAccountsByType("com.google");
    }

    public static HashMap<String, Object> convertToStringObjectMap(Map<String, String> map) {
        HashMap<String, Object> result = new HashMap<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                result.put(key, Boolean.parseBoolean(value));
            } else if (isDigitsOnly(value)) {
                result.put(key, Long.parseLong(value));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    public static boolean systemIsOnDarkMode() {
        int nightModeFlags =
                getAppContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
