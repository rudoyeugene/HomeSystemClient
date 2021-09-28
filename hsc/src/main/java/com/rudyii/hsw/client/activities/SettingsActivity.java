package com.rudyii.hsw.client.activities;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.CLIENTS_ROOT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_GLOBAL;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_ROOT;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.DELAYED_ARM_DELAY_SECS;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServer;
import static com.rudyii.hsw.client.helpers.Utils.getSimplifiedPrimaryAccountName;
import static com.rudyii.hsw.client.helpers.Utils.removeServerByKey;
import static com.rudyii.hsw.client.providers.DatabaseProvider.addOrUpdateServer;
import static com.rudyii.hsw.client.providers.DatabaseProvider.deleteIdFromSettings;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getAllServers;
import static com.rudyii.hsw.client.providers.DatabaseProvider.saveIntegerValueToSettingsStorage;
import static com.rudyii.hsw.client.providers.DatabaseProvider.setOrUpdateActiveServer;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getActiveServerRootReference;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

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
import com.rudyii.hs.common.objects.PairingData;
import com.rudyii.hs.common.objects.settings.GlobalSettings;
import com.rudyii.hs.common.type.NotificationType;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.helpers.Utils;
import com.rudyii.hsw.client.objects.internal.ServerData;

import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    public static final int CAMERA_SETTINGS_CODE = 1111;
    @SuppressWarnings("FieldCanBeLocal")
    private Button addServerButton, removeServerButton, cameraSettings;
    private SwitchCompat switchCollectStatsEnabled, switchMonitoringEnabled, switchHourlyReportEnabled, switchHourlyReportForced, switchVerboseOutputEnabled, switchShowMotionAreaEnabled;
    private EditText editTextForDelayedArmInterval, editTextForTextViewKeepDays;
    private DatabaseReference optionsReference;
    private ValueEventListener optionsValueEventListener;
    private GlobalSettings globalSettingsCopy;
    private int globalSettingsChangeId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Settings Activity created");
        String activeServerAlias = getActiveServer() == null ? " ... " : getActiveServer().getServerAlias();
        setTitle(getTitle() + activeServerAlias);
        setContentView(R.layout.activity_settings);

        buildAddServerButton();
        buildRemoveServerButton();
        buildOptionsControls();
        buildCameraSettingsButton();
        deactivateOptionsControls();
    }


    @Override
    protected void onStop() {
        super.onStop();

        optionsReference.removeEventListener(optionsValueEventListener);
        pushOptions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateOptions();
    }

    private void buildRemoveServerButton() {
        removeServerButton = findViewById(R.id.buttonUnpairServer);
        removeServerButton.setText(getResources().getString(R.string.button_pair_server_unpair_server));
        removeServerButton.setOnClickListener(v -> {
            AlertDialog.Builder unpairServerAlert = new AlertDialog.Builder(this);
            unpairServerAlert.setTitle(getResources().getString(R.string.dialog_server_unpair_alert_title));
            unpairServerAlert.setMessage(getResources().getString(R.string.dialog_server_unpair_alert_message));

            unpairServerAlert.setPositiveButton(getResources().getString(R.string.dialog_yes), (dialogInterface, i) -> {
                String accountName = getSimplifiedPrimaryAccountName();
                getActiveServerRootReference().child(CLIENTS_ROOT).child(accountName).removeValue();

                removeServerByKey(getActiveServer().getServerKey());
                deleteIdFromSettings(Utils.ACTIVE_SERVER);
                Map<String, ServerData> allServers = getAllServers();

                if (allServers.isEmpty()) {
                    new ToastDrawer().showToast(getResources().getString(R.string.no_paired_servers_found));
                } else {
                    ServerData serverData = allServers.values().stream().findFirst().get();
                    setOrUpdateActiveServer(serverData);
                    new ToastDrawer().showToast(String.format("%s %s", getResources().getString(R.string.switched_active_server_to), getActiveServer().getServerAlias()));
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

    private void buildOptionsControls() {
        switchCollectStatsEnabled = findViewById(R.id.switchCollectStatsEnabled);
        switchCollectStatsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            globalSettingsCopy.setGatherStats(isChecked);
        });

        switchMonitoringEnabled = findViewById(R.id.switchMonitoringEnabled);
        switchMonitoringEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            globalSettingsCopy.setMonitoringEnabled(isChecked);
        });

        switchHourlyReportEnabled = findViewById(R.id.switchHourlyReportEnabled);
        switchHourlyReportEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            globalSettingsCopy.setHourlyReportEnabled(isChecked);
        });

        switchHourlyReportForced = findViewById(R.id.switchHourlyReportForced);
        switchHourlyReportForced.setOnCheckedChangeListener((buttonView, isChecked) -> {
            globalSettingsCopy.setHourlyReportForced(isChecked);
        });

        switchVerboseOutputEnabled = findViewById(R.id.switchVerboseOutputEnabled);
        switchVerboseOutputEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            globalSettingsCopy.setVerboseOutput(isChecked);
        });

        switchShowMotionAreaEnabled = findViewById(R.id.switchShowMotionAreaEnabled);
        switchShowMotionAreaEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            globalSettingsCopy.setShowMotionArea(isChecked);
        });

        editTextForDelayedArmInterval = findViewById(R.id.editTextForDelayedArmInterval);
        editTextForDelayedArmInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextForDelayedArmInterval.setTextColor(getAppContext().getColor(R.color.textColor));
        editTextForDelayedArmInterval.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                globalSettingsCopy.setDelayedArmTimeout(Integer.parseInt(String.valueOf("".contentEquals(s) ? "0" : s)));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editTextForTextViewKeepDays = findViewById(R.id.editTextForTextViewKeepDays);
        editTextForTextViewKeepDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextForTextViewKeepDays.setTextColor(getAppContext().getColor(R.color.textColor));
        editTextForTextViewKeepDays.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                globalSettingsCopy.setHistoryDays(Integer.parseInt(String.valueOf("".contentEquals(s) ? "0" : s)));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void buildCameraSettingsButton() {
        cameraSettings = findViewById(R.id.buttonCameraSettings);
        cameraSettings.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), SelectCameraActivity.class));
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
    }

    private void updateOptions() {
        optionsValueEventListener = buildOptionsValueEventListener();
        optionsReference = getActiveServerRootReference().child(SETTINGS_ROOT).child(SETTINGS_GLOBAL);
        optionsReference.addValueEventListener(optionsValueEventListener);
    }

    private void pushOptions() {
        int currentGlobalSettingsChangeId = 0;
        if (globalSettingsCopy != null) {
            currentGlobalSettingsChangeId = globalSettingsCopy.hashCode();
        }

        if (globalSettingsChangeId != currentGlobalSettingsChangeId) {
            saveIntegerValueToSettingsStorage(DELAYED_ARM_DELAY_SECS, globalSettingsCopy.getDelayedArmTimeout());
            optionsReference.setValue(globalSettingsCopy);
        }
    }

    private ValueEventListener buildOptionsValueEventListener() {
        return new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @SuppressWarnings("unchecked")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    updateGlobalSettingsCopy(dataSnapshot.getValue(GlobalSettings.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
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

        }
    }

    private void updateGlobalSettingsCopy(GlobalSettings globalSettings) {
        this.globalSettingsCopy = globalSettings;
        this.globalSettingsChangeId = globalSettingsCopy.hashCode();
        updateSettingsControls();
    }

    private void updateSettingsControls() {
        switchCollectStatsEnabled.setChecked(globalSettingsCopy.isGatherStats());
        switchCollectStatsEnabled.setEnabled(true);

        switchMonitoringEnabled.setChecked(globalSettingsCopy.isMonitoringEnabled());
        switchMonitoringEnabled.setEnabled(true);

        switchHourlyReportEnabled.setChecked(globalSettingsCopy.isHourlyReportEnabled());
        switchHourlyReportEnabled.setEnabled(true);

        switchHourlyReportForced.setChecked(globalSettingsCopy.isHourlyReportForced());
        switchHourlyReportForced.setEnabled(true);

        switchVerboseOutputEnabled.setChecked(globalSettingsCopy.isVerboseOutput());
        switchVerboseOutputEnabled.setEnabled(true);

        switchShowMotionAreaEnabled.setChecked(globalSettingsCopy.isShowMotionArea());
        switchShowMotionAreaEnabled.setEnabled(true);

        editTextForDelayedArmInterval.setText(globalSettingsCopy.getDelayedArmTimeout() + "");
        editTextForDelayedArmInterval.setEnabled(true);

        editTextForTextViewKeepDays.setText(globalSettingsCopy.getHistoryDays() + "");
        editTextForTextViewKeepDays.setEnabled(true);
    }
}
