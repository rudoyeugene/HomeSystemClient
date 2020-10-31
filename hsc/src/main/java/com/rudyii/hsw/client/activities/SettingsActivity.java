package com.rudyii.hsw.client.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.helpers.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.media.RingtoneManager.ACTION_RINGTONE_PICKER;
import static android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TITLE;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TYPE;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getToken;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.HEALTH_CHECK_ENABLED;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.INTERVAL;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.MOTION_AREA;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.NOISE_LEVEL;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.REBOOT_TIMEOUT;
import static com.rudyii.hsw.client.helpers.Utils.DELAYED_ARM_DELAY_SECS;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServerAlias;
import static com.rudyii.hsw.client.helpers.Utils.getDeviceId;
import static com.rudyii.hsw.client.helpers.Utils.getLooper;
import static com.rudyii.hsw.client.helpers.Utils.getSoundNameBy;
import static com.rudyii.hsw.client.helpers.Utils.isPaired;
import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServer;
import static com.rudyii.hsw.client.helpers.Utils.removeServerFromServersList;
import static com.rudyii.hsw.client.helpers.Utils.serverKeyIsValid;
import static com.rudyii.hsw.client.helpers.Utils.stringIsEmptyOrNull;
import static com.rudyii.hsw.client.providers.DatabaseProvider.deleteIdFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveStringValueToSettings;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class SettingsActivity extends AppCompatActivity {
    public static final int CAMERA_SETTINGS_CODE = 444;
    private static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private final int QR_SCAN_CODE = 111;
    private final int INFORMATION_NOTIFICATION_SOUND_CODE = 222;
    private final int MOTION_NOTIFICATION_SOUND_CODE = 333;
    @SuppressWarnings("FieldCanBeLocal")
    private Button addServerButton, removeServerButton, infoSoundButton, motionSoundButton, cameraSettings;
    private Switch switchCollectStatsEnabled, switchMonitoringEnabled, switchHourlyReportEnabled, switchHourlyReportForced, switchVerboseOutputEnabled, switchShowMotionAreaEnabled;
    private EditText editTextForDelayedArmInterval, editTextForTextViewKeepDays, editTextForTextViewRecordInterval;
    private DatabaseReference optionsReference;
    private ValueEventListener optionsValueEventListener;
    private Map<String, Object> options;
    private boolean optionsChanged = false;
    private boolean buttonsChangedByUser = true;
    private Intent cameraSettingActivityIntent;
    private boolean cameraSettingsChanged = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Settings Activity created");

        setContentView(R.layout.activity_settings);
        buildSoundSelectionButtons();

        buildAppsListSpinner();

        buildAddServerButton();

        buildRemoveServerButton();

        resolveOptionsControls();
        deactivateOptionsControls();
        updateOptions();
    }

    private void buildCamerasSpinner() {
        Spinner cameras = findViewById(R.id.spinnerCameras);
        cameraSettings = findViewById(R.id.buttonCameraSettings);

        cameraSettings.setOnClickListener(view1 -> {
            startActivityForResult(cameraSettingActivityIntent, CAMERA_SETTINGS_CODE);
        });

        final ArrayList<String> cameraNames = new ArrayList<>();

        Map<String, Map<String, Object>> camerasSettings = (Map<String, Map<String, Object>>) options.get("cameras");

        for (Map.Entry<String, Map<String, Object>> entrySet : camerasSettings.entrySet()) {
            cameraNames.add(entrySet.getKey());
        }


        ArrayAdapter<String> camerasArray = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, cameraNames);
        camerasArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cameras.setAdapter(camerasArray);

        final int[] currentItem = new int[1];
        try {
            currentItem[0] = camerasArray.getPosition(cameraNames.get(0));
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, "No cameras found");
        }

        cameras.setSelection(currentItem[0]);
        cameras.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View convertView, int selected, long current) {
                String cameraName = (String) parent.getItemAtPosition(selected);
                buildCameraSettingActivityIntent(cameraName);

                ((TextView) convertView).setText(cameraName);

                if (currentItem[0] != selected) {
                    currentItem[0] = selected;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @NonNull
    private void buildCameraSettingActivityIntent(String cameraName) {
        cameraSettingActivityIntent = new Intent(getApplicationContext(), CameraSettingsActivity.class);
        Bundle dataBundle = new Bundle();
        Map<String, Object> cameraOptions = (Map<String, Object>) ((Map<String, Object>) options.get("cameras")).get(cameraName);

        dataBundle.putString("cameraName", cameraName);
        for (Map.Entry<String, Object> entry : cameraOptions.entrySet()) {
            dataBundle.putString(entry.getKey(), entry.getValue().toString());
        }

        cameraSettingActivityIntent.putExtras(dataBundle);
    }

    private void buildRemoveServerButton() {
        removeServerButton = findViewById(R.id.buttonUnpairServer);
        removeServerButton.setText(getResources().getString(R.string.button_pair_server_unpair_server));
        removeServerButton.setOnClickListener(v -> {
            AlertDialog.Builder unpairServerAlert = new AlertDialog.Builder(SettingsActivity.this);
            unpairServerAlert.setTitle(getResources().getString(R.string.dialog_server_unpair_alert_title));
            unpairServerAlert.setMessage(getResources().getString(R.string.dialog_server_unpair_alert_message));

            unpairServerAlert.setPositiveButton(getResources().getString(R.string.dialog_yes), (dialogInterface, i) -> {
                String accountName = getDeviceId();
                if (!stringIsEmptyOrNull(accountName)) {
                    getRootReference().child("/connectedClients/" + accountName).removeValue();
                }

                removeServerFromServersList(getActiveServerAlias());
                deleteIdFromSettings(Utils.ACTIVE_SERVER);

                new ToastDrawer().showToast(isPaired() ? getActiveServerAlias() + ": " + getResources().getString(R.string.toast_server_unpair_failure) : getResources().getString(R.string.toast_server_unpair_success));
                addServerButton.setText(R.string.button_pair_server_pair_server);
            });

            unpairServerAlert.setNegativeButton(getResources().getString(R.string.dialog_no), (dialogInterface, i) -> {

            });

            unpairServerAlert.show();
        });
    }

    private void buildAddServerButton() {
        addServerButton = findViewById(R.id.buttonPairServer);
        addServerButton.setText(getResources().getString(R.string.button_pair_server_pair_server));
        addServerButton.setTextColor(getAppContext().getColor(R.color.textColor));
        addServerButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ACTION_SCAN);
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, QR_SCAN_CODE);
            } catch (ActivityNotFoundException anfe) {
                showDialogToDownloadQrCodeScanner(SettingsActivity.this);
            }
        });
    }

    private void buildSoundSelectionButtons() {
        infoSoundButton = findViewById(R.id.buttonInfoSound);
        infoSoundButton.setTextColor(getAppContext().getColor(R.color.textColor));
        infoSoundButton.setText(getSoundNameBy(getStringValueFromSettings(Utils.INFO_SOUND)));

        infoSoundButton.setOnClickListener(v -> {
            Intent infoSoundIntent = new Intent(ACTION_RINGTONE_PICKER);
            infoSoundIntent.putExtra(EXTRA_RINGTONE_TYPE, TYPE_NOTIFICATION);
            infoSoundIntent.putExtra(EXTRA_RINGTONE_PICKED_URI, (Uri) null);
            infoSoundIntent.putExtra(EXTRA_RINGTONE_TITLE, "Select Tone");
            infoSoundIntent.putExtra(EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            startActivityForResult(infoSoundIntent, INFORMATION_NOTIFICATION_SOUND_CODE);
        });

        motionSoundButton = findViewById(R.id.buttonMotionSound);
        motionSoundButton.setTextColor(getAppContext().getColor(R.color.textColor));
        motionSoundButton.setText(getSoundNameBy(getStringValueFromSettings(Utils.MOTION_SOUND)));

        motionSoundButton.setOnClickListener(v -> {
            Intent infoSoundIntent = new Intent(ACTION_RINGTONE_PICKER);
            infoSoundIntent.putExtra(EXTRA_RINGTONE_TYPE, TYPE_NOTIFICATION);
            infoSoundIntent.putExtra(EXTRA_RINGTONE_PICKED_URI, (Uri) null);
            infoSoundIntent.putExtra(EXTRA_RINGTONE_TITLE, "Select Tone");
            infoSoundIntent.putExtra(EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            startActivityForResult(infoSoundIntent, MOTION_NOTIFICATION_SOUND_CODE);
        });
    }

    private void buildAppsListSpinner() {
        final ArrayList<ResolveInfoWrapper> infoWrappers = new ArrayList<>();

        Spinner appsList = findViewById(R.id.spinnerAppsList);
        final ActivityAdapter arrayAdapter = new ActivityAdapter(getApplicationContext(), infoWrappers) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null)
                    convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_spinner_item, parent, false);

                ((TextView) convertView).setText(getCameraAppName());
                ((TextView) convertView).setTextColor(getAppContext().getColor(R.color.textColor));

                return convertView;
            }
        };

        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Handler handler = new Handler(getLooper());
        handler.post(() -> {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> pkgAppsList = getPackageManager().queryIntentActivities(mainIntent, 0);

            Collections.sort(pkgAppsList, new ResolveInfo.DisplayNameComparator(getPackageManager()));

            for (ResolveInfo resolveInfo : pkgAppsList) {
                infoWrappers.add(new ResolveInfoWrapper(resolveInfo));
            }

            runOnUiThread(arrayAdapter::notifyDataSetChanged);
        });

        appsList.setAdapter(arrayAdapter);
        appsList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean init;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int selected, long current) {
                if (!init) {
                    init = true;
                    return;
                }

                ResolveInfoWrapper info = (ResolveInfoWrapper) parent.getItemAtPosition(selected);
                String packageName = info.getInfo().activityInfo.packageName;

                saveStringValueToSettings(Utils.CAMERA_APP, packageName);

                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void resolveOptionsControls() {
        switchCollectStatsEnabled = findViewById(R.id.switchCollectStatsEnabled);
        switchCollectStatsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonsChangedByUser) {
                optionsChanged = true;
                options.put("collectStatistics", isChecked);
            }
        });

        switchMonitoringEnabled = findViewById(R.id.switchMonitoringEnabled);
        switchMonitoringEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonsChangedByUser) {
                optionsChanged = true;
                options.put("monitoringEnabled", isChecked);
            }
        });

        switchHourlyReportEnabled = findViewById(R.id.switchHourlyReportEnabled);
        switchHourlyReportEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonsChangedByUser) {
                optionsChanged = true;
                options.put("hourlyReportEnabled", isChecked);
            }
        });

        switchHourlyReportForced = findViewById(R.id.switchHourlyReportForced);
        switchHourlyReportForced.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonsChangedByUser) {
                optionsChanged = true;
                options.put("hourlyReportForced", isChecked);
            }
        });

        switchVerboseOutputEnabled = findViewById(R.id.switchVerboseOutputEnabled);
        switchVerboseOutputEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonsChangedByUser) {
                optionsChanged = true;
                options.put("verboseOutputEnabled", isChecked);
            }
        });

        switchShowMotionAreaEnabled = findViewById(R.id.switchShowMotionAreaEnabled);
        switchShowMotionAreaEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonsChangedByUser) {
                optionsChanged = true;
                options.put("showMotionArea", isChecked);
            }
        });

        editTextForDelayedArmInterval = findViewById(R.id.editTextForDelayedArmInterval);
        editTextForDelayedArmInterval.setTextColor(getAppContext().getColor(R.color.textColor));
        editTextForDelayedArmInterval.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                optionsChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editTextForTextViewKeepDays = findViewById(R.id.editTextForTextViewKeepDays);
        editTextForTextViewKeepDays.setTextColor(getAppContext().getColor(R.color.textColor));
        editTextForTextViewKeepDays.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                optionsChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editTextForTextViewRecordInterval = findViewById(R.id.editTextForTextViewRecordInterval);
        editTextForTextViewRecordInterval.setTextColor(getAppContext().getColor(R.color.textColor));
        editTextForTextViewRecordInterval.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                optionsChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void deactivateOptionsControls() {
        switchCollectStatsEnabled.setEnabled(false);
        switchMonitoringEnabled.setEnabled(false);
        switchHourlyReportEnabled.setEnabled(false);
        switchHourlyReportForced.setEnabled(false);
        switchVerboseOutputEnabled.setEnabled(false);
        switchShowMotionAreaEnabled.setEnabled(false);
        editTextForDelayedArmInterval.setEnabled(false);
        editTextForTextViewKeepDays.setEnabled(false);
        editTextForTextViewRecordInterval.setEnabled(false);
    }

    @Override
    public void onPause() {
        super.onPause();

        optionsReference.removeEventListener(optionsValueEventListener);

        if (optionsChanged || cameraSettingsChanged) {
            pushOptions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        optionsReference.removeEventListener(optionsValueEventListener);

        if (optionsChanged || cameraSettingsChanged) {
            pushOptions();
        }
    }

    private void updateOptions() {
        optionsValueEventListener = buildOptionsValueEventListener();
        optionsReference = getRootReference().child("/options");
        optionsReference.addValueEventListener(optionsValueEventListener);
    }

    private void pushOptions() {
        options.put("delayedArmInterval", Long.valueOf(editTextForDelayedArmInterval.getText().toString()));
        saveStringValueToSettings(DELAYED_ARM_DELAY_SECS, editTextForDelayedArmInterval.getText().toString());
        options.put("keepDays", Long.valueOf(editTextForTextViewKeepDays.getText().toString()));
        options.put("recordInterval", Long.valueOf(editTextForTextViewRecordInterval.getText().toString()));

        optionsReference.setValue(options);
    }

    private ValueEventListener buildOptionsValueEventListener() {
        return new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @SuppressWarnings("unchecked")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                options = (Map<String, Object>) dataSnapshot.getValue();

                if (options == null) return;

                buttonsChangedByUser = false;

                buildCamerasSpinner();

                switchCollectStatsEnabled.setChecked((boolean) options.get("collectStatistics"));
                switchCollectStatsEnabled.setEnabled(true);

                switchMonitoringEnabled.setChecked((boolean) options.get("monitoringEnabled"));
                switchMonitoringEnabled.setEnabled(true);

                switchHourlyReportEnabled.setChecked((boolean) options.get("hourlyReportEnabled"));
                switchHourlyReportEnabled.setEnabled(true);

                switchHourlyReportForced.setChecked((boolean) options.get("hourlyReportForced"));
                switchHourlyReportForced.setEnabled(true);

                switchVerboseOutputEnabled.setChecked((boolean) options.get("verboseOutputEnabled"));
                switchVerboseOutputEnabled.setEnabled(true);

                switchShowMotionAreaEnabled.setChecked((boolean) options.get("showMotionArea"));
                switchShowMotionAreaEnabled.setEnabled(true);

                editTextForDelayedArmInterval.setText("" + (long) options.get("delayedArmInterval"));
                editTextForDelayedArmInterval.setEnabled(true);

                editTextForTextViewKeepDays.setText("" + (long) options.get("keepDays"));
                editTextForTextViewKeepDays.setEnabled(true);

                editTextForTextViewRecordInterval.setText("" + (long) options.get("recordInterval"));
                editTextForTextViewRecordInterval.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void showDialogToDownloadQrCodeScanner(final AppCompatActivity act) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(getResources().getString(R.string.dialog_download_qr_scanner_title));
        downloadDialog.setMessage(getResources().getString(R.string.dialog_download_qr_scanner_message));
        downloadDialog.setPositiveButton(getResources().getString(R.string.dialog_yes), (dialogInterface, i) -> {
            Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                act.startActivity(intent);
            } catch (ActivityNotFoundException anfe) {
                Log.e(TAG, "Failed to open Play Store.");
            }
        });
        downloadDialog.setNegativeButton(getResources().getString(R.string.dialog_no), (dialogInterface, i) -> {

        });
        downloadDialog.show();
    }

    private String getCameraAppPackageName() {
        return getStringValueFromSettings(Utils.CAMERA_APP);
    }

    private String getCameraAppName() {
        String appName = getResources().getString(R.string.text_textViewSelectCameraApp);
        try {
            appName = String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getCameraAppPackageName(), PackageManager.GET_META_DATA)));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to load package name");
        }

        return appName;
    }

    @SuppressWarnings("unchecked")
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (intent == null) {
            return;
        }

        String contents, soundName;
        Uri soundUri;
        switch (requestCode) {
            case QR_SCAN_CODE:
                contents = intent.getStringExtra("SCAN_RESULT");
                ArrayList<String> serverData = new ArrayList<>(asList(contents.split(":")));

                String serverAlias = serverData.get(0);
                String serverKey = serverData.get(1);

                String serverList = getStringValueFromSettings(Utils.SERVER_LIST);
                Gson gson = new Gson();
                HashMap<String, String> serverListMap;

                if (stringIsEmptyOrNull(serverList)) {
                    serverListMap = new HashMap<>();
                    serverListMap.put(serverAlias, serverKey);
                    saveStringValueToSettings(Utils.SERVER_LIST, gson.toJson(serverListMap));
                } else {
                    serverListMap = gson.fromJson(serverList, HashMap.class);
                    serverListMap.put(serverAlias, serverKey);
                    saveStringValueToSettings(Utils.SERVER_LIST, gson.toJson(serverListMap));
                }

                if (serverKeyIsValid(serverKey)) {
                    saveStringValueToSettings(Utils.ACTIVE_SERVER, serverAlias);
                    registerUserDataOnServer(serverKey, serverAlias, getToken());

                    new ToastDrawer().showToast(isPaired() ? getResources().getString(R.string.toast_server_paired_success) : getResources().getString(R.string.toast_server_paired_failure));
                } else {
                    new ToastDrawer().showToast(getResources().getString(R.string.toast_server_paired_failure_detailed));
                }

                break;

            case INFORMATION_NOTIFICATION_SOUND_CODE:
                soundUri = (Uri) requireNonNull(intent.getExtras()).get("android.intent.extra.ringtone.PICKED_URI");

                if (soundUri == null) {
                    deleteIdFromSettings(Utils.INFO_SOUND);
                    soundName = getSoundNameBy(getStringValueFromSettings(Utils.INFO_SOUND));
                    new ToastDrawer().showToast(getResources().getString(R.string.toast_info_sound_removed));
                } else {
                    saveStringValueToSettings(Utils.INFO_SOUND, soundUri.toString());
                    soundName = getSoundNameBy(getStringValueFromSettings(Utils.INFO_SOUND));
                    new ToastDrawer().showToast(getResources().getString(R.string.toast_info_sound_changed_to) + soundName);
                }
                infoSoundButton.setText(soundName);
                break;

            case MOTION_NOTIFICATION_SOUND_CODE:
                soundUri = (Uri) requireNonNull(intent.getExtras()).get("android.intent.extra.ringtone.PICKED_URI");

                if (soundUri == null) {
                    deleteIdFromSettings(Utils.MOTION_SOUND);
                    soundName = getSoundNameBy(getStringValueFromSettings(Utils.MOTION_SOUND));
                    new ToastDrawer().showToast(getResources().getString(R.string.toast_motion_sound_removed));
                } else {
                    saveStringValueToSettings(Utils.MOTION_SOUND, soundUri.toString());
                    soundName = getSoundNameBy(getStringValueFromSettings(Utils.MOTION_SOUND));
                    new ToastDrawer().showToast(getResources().getString(R.string.toast_motion_sound_changed_to) + soundName);
                }
                motionSoundButton.setText(soundName);
                break;

            case CAMERA_SETTINGS_CODE:
                cameraSettingsChanged = true;

                Map<String, Object> cameraSettings = (Map<String, Object>) ((Map<String, Object>) options.get("cameras")).get(intent.getStringExtra("cameraName"));

                cameraSettings.put(HEALTH_CHECK_ENABLED, intent.getBooleanExtra(HEALTH_CHECK_ENABLED, true));
                cameraSettings.put(INTERVAL, intent.getLongExtra(INTERVAL, 500L));
                cameraSettings.put(MOTION_AREA, intent.getLongExtra(MOTION_AREA, 20L));
                cameraSettings.put(NOISE_LEVEL, intent.getLongExtra(NOISE_LEVEL, 5L));
                cameraSettings.put(REBOOT_TIMEOUT, intent.getLongExtra(REBOOT_TIMEOUT, 60000L));

                break;
        }
    }

    private final class ResolveInfoWrapper {
        private final ResolveInfo mInfo;

        ResolveInfoWrapper(ResolveInfo info) {
            mInfo = info;
        }

        @Override
        public String toString() {
            return mInfo.loadLabel(getPackageManager()).toString();
        }

        ResolveInfo getInfo() {
            return mInfo;
        }
    }

    private class ActivityAdapter extends ArrayAdapter<ResolveInfoWrapper> {
        private final LayoutInflater mInflater;

        ActivityAdapter(Context context, ArrayList<ResolveInfoWrapper> list) {
            super(context, android.R.layout.simple_spinner_item, list);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final ResolveInfoWrapper info = getItem(position);

            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                view.setTag(view.findViewById(android.R.id.text1));
            }

            final TextView textView = (TextView) view.getTag();
            textView.setTextColor(getAppContext().getColor(R.color.textColor));
            textView.setText(requireNonNull(info).getInfo().loadLabel(getPackageManager()));

            return view;
        }
    }
}
