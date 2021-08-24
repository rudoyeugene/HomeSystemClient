package com.rudyii.hsw.client.helpers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.providers.DatabaseProvider.addOrUpdateServer;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getAllServers;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveStringValueToSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.setOrUpdateActiveServer;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getCustomReference;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.objects.ServerData;
import com.rudyii.hsw.client.providers.DatabaseProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by Jack on 18.12.2017.
 */

@SuppressWarnings("WeakerAccess")
public class Utils {
    public static final String DELAYED_ARM_DELAY_SECS = "DELAYED_ARM_DELAY_SECS";
    public static final String ACTIVE_SERVER = "ACTIVE_SERVER";
    public static final String CAMERA_APP = "CAMERA_APP";
    public static final Locale currentLocale = getAppContext().getResources().getConfiguration().locale;
    private static final Gson gson = new Gson();
    private static final String INSTALLATION_ID = "INSTALLATION_ID";

    public static String getCurrentTimeAndDateDoubleDotsDelimFrom(Long timeStamp) {
        if (timeStamp == null) {
            return "";
        }

        Date date = new Date(timeStamp);
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy", currentLocale);
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
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

    public static String getDeviceId() {
        String installationId = getStringValueFromSettings(INSTALLATION_ID);

        if (stringIsEmptyOrNull(installationId)) {
            installationId = UUID.randomUUID().toString();
            saveStringValueToSettings(INSTALLATION_ID, installationId);
        }

        return installationId;
    }

    public static void registerUserDataOnServers(String token) {
        getAllServers().values().forEach(serverData -> registerUserDataOnServer(serverData, token));
    }

    public static void registerUserDataOnServer(ServerData serverData, String token) {
        String simplifiedPrimaryAccountName = getSimplifiedPrimaryAccountName();
        String deviceId = getDeviceId();

        HashMap<String, Object> clientData = new HashMap<>();

        clientData.put("notificationType", serverData.getNotificationType().getFirebaseName());
        clientData.put("notificationsMuted", serverData.isNotificationsMuted());
        clientData.put("hourlyReportMuted", serverData.isHourlyReportMuted());
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
                getCustomReference(serverData.getServerKey()).child("/connectedClients/" + deviceId).removeValue();
            }
            getCustomReference(serverData.getServerKey()).child("/connectedClients/" + simplifiedPrimaryAccountName).setValue(clientData);
        } else if (stringIsNotEmptyOrNull(deviceId)) {
            if (stringIsNotEmptyOrNull(simplifiedPrimaryAccountName)) {
                getCustomReference(serverData.getServerKey()).child("/connectedClients/" + simplifiedPrimaryAccountName).removeValue();
            }
            getCustomReference(serverData.getServerKey()).child("/connectedClients/" + deviceId).setValue(clientData);
        } else {
            new ToastDrawer().showToast(getAppContext().getResources().getString(R.string.toast_failed_to_register_on_server));
        }
    }

    public static String getSimplifiedPrimaryAccountName() {
        Account[] accounts = getAccounts();
        String simplifiedAccountName = "";

        if (accounts.length > 0) {
            Account mainAccount = accounts[0];
            simplifiedAccountName = mainAccount.name.split("@")[0].replace(".", "");

        }

        return simplifiedAccountName;
    }

    public static String getPrimaryAccountEmail() {
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

    public static void removeServerByKey(String serverKey) {
        DatabaseProvider.removeServer(serverKey);
    }

    public static ServerData getActiveServer() {
        return buildFromRawJson(getStringValueFromSettings(ACTIVE_SERVER), ServerData.class);
    }

    public static void updateServer(ServerData serverData) {
        addOrUpdateServer(serverData);
        setOrUpdateActiveServer(serverData);
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

    public static String writeJson(Object value) {
        return gson.toJson(value);
    }

    public static <T> T buildFromRawJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    @NonNull
    private static Account[] getAccounts() {
        return AccountManager.get(getAppContext()).getAccountsByType("com.google");
    }

    public static boolean systemIsOnDarkMode() {
        int nightModeFlags =
                getAppContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
