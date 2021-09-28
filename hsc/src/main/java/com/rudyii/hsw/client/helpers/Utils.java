package com.rudyii.hsw.client.helpers;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.CLIENTS_ROOT;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.providers.DatabaseProvider.addOrUpdateServer;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getAllServers;
import static com.rudyii.hsw.client.providers.DatabaseProvider.readStringValueFromSettingsStorage;
import static com.rudyii.hsw.client.providers.DatabaseProvider.setOrUpdateActiveServer;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getCustomRootReference;
import static com.rudyii.hsw.client.services.FCMMessagingService.FCM_TOKEN;

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

import com.google.gson.Gson;
import com.rudyii.hs.common.objects.ConnectedClient;
import com.rudyii.hs.common.type.NotificationType;
import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.objects.internal.ServerData;
import com.rudyii.hsw.client.providers.DatabaseProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

/**
 * Created by Jack on 18.12.2017.
 */

@SuppressWarnings("WeakerAccess")
public class Utils {
    public static final String DELAYED_ARM_DELAY_SECS = "DELAYED_ARM_DELAY_SECS";
    public static final String ACTIVE_SERVER = "ACTIVE_SERVER";
    public static final Locale currentLocale = getAppContext().getResources().getConfiguration().getLocales().get(0);
    private static final Gson gson = new Gson();
    private static HandlerThread handlerThread;

    public static String getCurrentTimeAndDateDoubleDotsDelimFrom(Long timeStamp) {
        if (timeStamp == null) {
            return "";
        }

        Date date = new Date(timeStamp);
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy", currentLocale);
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
    }

    public static String getSystemModeLocalized(SystemModeType systemModeType) {
        switch (systemModeType) {
            case AUTOMATIC:
                return getAppContext().getString(R.string.toggle_button_text_system_mode_automatic);
            case MANUAL:
                return getAppContext().getString(R.string.toggle_button_text_system_mode_manual);
            default:
                return getAppContext().getString(R.string.toggle_button_text_system_mode_or_state_uknown);
        }
    }

    public static String getSystemStateLocalized(SystemStateType systemStateType) {
        switch (systemStateType) {
            case ARMED:
                return getAppContext().getString(R.string.toggle_button_text_system_state_armed);
            case DISARMED:
                return getAppContext().getString(R.string.toggle_button_text_system_state_disarmed);
            case RESOLVING:
                return getAppContext().getString(R.string.toggle_button_text_system_state_resolving);
            default:
                return getAppContext().getString(R.string.toggle_button_text_system_mode_or_state_uknown);
        }
    }

    public static void registerUserDataOnServers() {
        getAllServers().values().forEach(Utils::registerUserDataOnServer);
    }

    public static void registerUserDataOnServer(ServerData serverData) {
        String simplifiedPrimaryAccountName = getSimplifiedPrimaryAccountName();

        PackageInfo pInfo;
        String version = "0.0.0";
        try {
            pInfo = getAppContext().getPackageManager().getPackageInfo(getAppContext().getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ConnectedClient connectedClient = ConnectedClient.builder()
                .appVersion(version)
                .device(android.os.Build.MODEL)
                .email(getPrimaryAccountEmail())
                .token(readStringValueFromSettingsStorage(FCM_TOKEN))
                .hourlyReportEnabled(serverData.isHourlyReportEnabled())
                .lastRegistration(System.currentTimeMillis())
                .notificationType(NotificationType.valueOf(serverData.getNotificationType().name()))
                .build();

        getCustomRootReference(serverData.getServerKey()).child(CLIENTS_ROOT).child(simplifiedPrimaryAccountName).setValue(connectedClient);
    }

    public static String getSimplifiedPrimaryAccountName() {
        return getPrimaryAccountEmail().split("@")[0].replace(".", "");
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

    public static void removeServerByKey(String serverKey) {
        DatabaseProvider.removeServer(serverKey);
    }

    public static ServerData getActiveServer() {
        return buildFromRawJson(readStringValueFromSettingsStorage(ACTIVE_SERVER), ServerData.class);
    }

    public static void updateServer(ServerData serverData) {
        addOrUpdateServer(serverData);
        setOrUpdateActiveServer(serverData);
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
        if (handlerThread == null || !handlerThread.isAlive()) {
            handlerThread = new HandlerThread("Thread: " + new Random().nextInt(1000));
            handlerThread.start();
        }
        return handlerThread.getLooper();
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

    public static <T> T buildFromStringMap(Map<String, String> props, Class<T> clazz) {
        return gson.fromJson(writeJson(props), clazz);
    }

    public static <T> T buildFromPropertiesMap(Map<String, String> props, Class<T> clazz) {
        return gson.fromJson(gson.toJson(props), clazz);
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

    public static String calculateUptimeFromMinutes(long totalMinutes) {
        Duration duration = Duration.ofMinutes(totalMinutes);
        long hours = duration.toHours();
        long days = duration.toDays();

        long leftMinutes = totalMinutes > 60 ? (totalMinutes - (hours * 60)) : totalMinutes;
        long leftHours = hours > 23 ? (hours - (days * 24)) : hours;

        StringBuilder stringBuilder = new StringBuilder();

        if (days == 1) {
            stringBuilder.append(days);
            stringBuilder.append(getAppContext().getResources().getString(R.string.text_day));
        }

        if (days > 1) {
            stringBuilder.append(days);
            stringBuilder.append(getAppContext().getResources().getString(R.string.text_days));
        }

        stringBuilder.append(String.format(Locale.getDefault(), "%02d:", leftHours))
                .append(String.format(Locale.getDefault(), "%02d", leftMinutes));

        return stringBuilder.toString();
    }
}
