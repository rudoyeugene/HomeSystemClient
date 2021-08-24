package com.rudyii.hsw.client.activities;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.CONTINUOUS_MONITORING;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.HEALTH_CHECK_ENABLED;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.INTERVAL;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.MOTION_AREA;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.NOISE_LEVEL;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.REBOOT_TIMEOUT;
import static com.rudyii.hsw.client.activities.CameraSettingsActivity.USE_MOTION_OBJECT;
import static com.rudyii.hsw.client.helpers.Utils.DELAYED_ARM_DELAY_SECS;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServer;
import static com.rudyii.hsw.client.helpers.Utils.getDeviceId;
import static com.rudyii.hsw.client.helpers.Utils.getLooper;
import static com.rudyii.hsw.client.helpers.Utils.removeServerByKey;
import static com.rudyii.hsw.client.helpers.Utils.stringIsEmptyOrNull;
import static com.rudyii.hsw.client.providers.DatabaseProvider.addOrUpdateServer;
import static com.rudyii.hsw.client.providers.DatabaseProvider.deleteIdFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getAllServers;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveStringValueToSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.setOrUpdateActiveServer;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;
import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.helpers.Utils;
import com.rudyii.hsw.client.objects.PairingData;
import com.rudyii.hsw.client.objects.ServerData;
import com.rudyii.hsw.client.objects.types.NotificationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    public static final int CAMERA_SETTINGS_CODE = 1111;
    @SuppressWarnings("FieldCanBeLocal")
    private Button addServerButton, removeServerButton, cameraSettings;
    private SwitchCompat switchCollectStatsEnabled, switchMonitoringEnabled, switchHourlyReportEnabled, switchHourlyReportForced, switchVerboseOutputEnabled, switchShowMotionAreaEnabled;
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
        String activeServerAlias = getActiveServer() == null ? " ... " : getActiveServer().getServerAlias();
        setTitle(getTitle() + activeServerAlias);
        setContentView(R.layout.activity_settings);

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
            AlertDialog.Builder unpairServerAlert = new AlertDialog.Builder(this);
            unpairServerAlert.setTitle(getResources().getString(R.string.dialog_server_unpair_alert_title));
            unpairServerAlert.setMessage(getResources().getString(R.string.dialog_server_unpair_alert_message));

            unpairServerAlert.setPositiveButton(getResources().getString(R.string.dialog_yes), (dialogInterface, i) -> {
                String accountName = getDeviceId();
                if (!stringIsEmptyOrNull(accountName)) {
                    getRootReference().child("/connectedClients/" + accountName).removeValue();
                }

                removeServerByKey(getActiveServer().getServerKey());
                deleteIdFromSettings(Utils.ACTIVE_SERVER);
                Map<String, ServerData> allServers = getAllServers();

                if (allServers.isEmpty()) {
                    new ToastDrawer().showToast(getResources().getString(R.string.no_paired_servers_found));
                } else {
                    ServerData serverData = allServers.values().stream().findFirst().get();
                    setOrUpdateActiveServer(serverData);
                    new ToastDrawer().showToast(getResources().getString(R.string.switched_active_server_to) + getActiveServer().getServerAlias());
                }
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
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
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

                buttonsChangedByUser = true;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
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

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String result = scanResult.getContents();
            Gson gson = new Gson();
            PairingData pairingData = gson.fromJson(result, PairingData.class);

            ServerData serverData = ServerData.builder()
                    .serverKey(pairingData.getServerKey())
                    .serverAlias(pairingData.getServerAlias())
                    .serverIp(pairingData.getServerIp())
                    .serverPort(pairingData.getServerPort())
                    .notificationType(NotificationType.ALL)
                    .build();
            addOrUpdateServer(serverData);
            setOrUpdateActiveServer(serverData);
            new ToastDrawer().showToast(getResources().getString(R.string.toast_server_paired_success));

        } else {
            switch (requestCode) {
                case CAMERA_SETTINGS_CODE:
                    cameraSettingsChanged = true;

                    Map<String, Object> cameraSettings = (Map<String, Object>) ((Map<String, Object>) options.get("cameras")).get(intent.getStringExtra("cameraName"));

                    cameraSettings.put(HEALTH_CHECK_ENABLED, intent.getBooleanExtra(HEALTH_CHECK_ENABLED, true));
                    cameraSettings.put(USE_MOTION_OBJECT, intent.getBooleanExtra(USE_MOTION_OBJECT, true));
                    cameraSettings.put(CONTINUOUS_MONITORING, intent.getBooleanExtra(CONTINUOUS_MONITORING, false));
                    cameraSettings.put(INTERVAL, intent.getLongExtra(INTERVAL, 500L));
                    cameraSettings.put(MOTION_AREA, intent.getLongExtra(MOTION_AREA, 20L));
                    cameraSettings.put(NOISE_LEVEL, intent.getLongExtra(NOISE_LEVEL, 5L));
                    cameraSettings.put(REBOOT_TIMEOUT, intent.getLongExtra(REBOOT_TIMEOUT, 60000L));

                    break;
            }
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
