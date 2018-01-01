package com.rudyii.hsw.client.helpers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.rudyii.hsw.client.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;

/**
 * Created by j-a-c on 18.12.2017.
 */

public class Utils {
    public static Locale currentLocale = getAppContext().getResources().getConfiguration().locale;

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
            return null != UUID.fromString(serverKey);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getServerKey() {
        return getStringValueFromSettings("SERVER_KEY");
    }

    public static HashMap<String, Object> buildDataForMainActivityFrom(String mode, String state, Boolean portsState) {
        HashMap<String, Object> result = new HashMap<>();
        switch (mode.toLowerCase()) {
            case "automatic":
                result.put("systemModeText", getAppContext().getResources().getString(R.string.toggle_button_system_mode_automatic_text).toUpperCase());
                break;
            case "manual":
                result.put("systemModeText", getAppContext().getResources().getString(R.string.toggle_button_system_mode_manual_text).toUpperCase());
                break;
            default:
                result.put("systemModeText", "UNKNOWN_MODE");
        }

        switch (state.toLowerCase()) {
            case "armed":
                result.put("systemStateText", getAppContext().getResources().getString(R.string.toggle_button_system_state_armed_text).toUpperCase());
                break;
            case "disarmed":
                result.put("systemStateText", getAppContext().getResources().getString(R.string.toggle_button_system_state_disarmed_text).toUpperCase());
                break;
            case "auto":
                result.put("systemStateText", getAppContext().getResources().getString(R.string.toggle_button_system_state_auto_text).toUpperCase());
                break;
            default:
                result.put("systemStateText", "UNKNOWN_STATE");
        }

        result.put("portsState", portsState);

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

    public static String getSimplifiedPrimaryAccountName() {
        AccountManager accountManager = AccountManager.get(getAppContext());
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String simplifiedAccountName = "";

        if (accounts.length > 0) {
            Account mainAccount = accounts[0];
            simplifiedAccountName = mainAccount.name.split("@")[0].replace(".", "");

        }

        return simplifiedAccountName;
    }


    public static void saveImageFromCamera(Bitmap bitmap, String cameraName, String imageName) {
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
}
