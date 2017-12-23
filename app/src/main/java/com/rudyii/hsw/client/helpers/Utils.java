package com.rudyii.hsw.client.helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;

/**
 * Created by j-a-c on 18.12.2017.
 */

public class Utils {

    public static String getCurrentTimeAndDateDoubleDotsDelimFrom(Long timeStamp) {
        if (timeStamp == null) {
            return "";
        }

        Date date = new Date(timeStamp);
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
    }

    public static String getCurrentTimeAndDateSingleDotDelimFrom(Long timeStamp) {
        if (timeStamp == null) {
            return "";
        }

        Date date = new Date(timeStamp);
        DateFormat dateFormat = new SimpleDateFormat("HH.mm.ss-dd.MM.yyyy");
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
    }

    public static String getSoundNameBy(String soundUri) {
        Ringtone ringtone = RingtoneManager.getRingtone(getAppContext(), Uri.parse(soundUri));
        return ringtone.getTitle(getAppContext());
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPaired() {
        String serverKey = getStringValueFromSettings("SERVER_KEY");
        return serverKeyIsValid(serverKey);
    }

    public static boolean serverKeyIsValid(String serverKey) {
        try {
            UUID.fromString(serverKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getServerKey() {
        return getStringValueFromSettings("SERVER_KEY");
    }

    public static HashMap<String, Object> buildMainActivityButtonsStateMapFrom(String mode, String state) {
        HashMap<String, Object> result = new HashMap<>();

        if (mode.equalsIgnoreCase("automatic") && state.equalsIgnoreCase("armed")) {
            result.put("systemModeChecked", true);
            result.put("systemStateChecked", true);
            result.put("systemStateEnabled", false);
        } else if (mode.equalsIgnoreCase("automatic") && (state.equalsIgnoreCase("disarmed") || state.equalsIgnoreCase("auto"))) {
            result.put("systemModeChecked", true);
            result.put("systemStateChecked", false);
            result.put("systemStateEnabled", false);
        } else if (!mode.equalsIgnoreCase("automatic") && state.equalsIgnoreCase("armed")) {
            result.put("systemModeChecked", false);
            result.put("systemStateChecked", true);
            result.put("systemStateEnabled", true);
        } else if (!mode.equalsIgnoreCase("automatic") && state.equalsIgnoreCase("disarmed")) {
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
}
