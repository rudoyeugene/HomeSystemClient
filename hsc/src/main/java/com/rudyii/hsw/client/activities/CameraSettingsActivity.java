package com.rudyii.hsw.client.activities;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_CAMERA;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_ROOT;
import static com.rudyii.hsw.client.helpers.Utils.buildFromRawJson;
import static com.rudyii.hsw.client.objects.internal.CameraSettingsInternal.CAMERA_SETTINSG_EXTRA_DATA_NAME;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getActiveServerRootReference;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.rudyii.hs.common.objects.settings.CameraSettings;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.objects.internal.CameraSettingsInternal;

public class CameraSettingsActivity extends AppCompatActivity {
    private CameraSettings cameraSettingsCopy;
    private int cameraSettingsChangeId;
    private String cameraName;
    private SwitchCompat switchHealthCheckEnabled, switchUseMotionObject, switchContinuousMonitoring;
    private EditText editTextForMotionInterval, editTextForMotionArea, editTextForNoiseLevel, editTextForRebootTimeout, editTextForRecordLength;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_settings);

        CameraSettingsInternal cameraSettingsInternal = buildFromRawJson(getIntent().getStringExtra(CAMERA_SETTINSG_EXTRA_DATA_NAME), CameraSettingsInternal.class);
        this.cameraName = cameraSettingsInternal.getCameraName();
        this.setTitle(getTitle() + cameraName);
        this.cameraSettingsCopy = cameraSettingsInternal.getCameraSettings();
        this.cameraSettingsChangeId = cameraSettingsCopy.hashCode();

        switchHealthCheckEnabled = findViewById(R.id.healthCheckEnabled);
        switchHealthCheckEnabled.setChecked(cameraSettingsCopy.isHealthCheckEnabled());
        switchHealthCheckEnabled.setEnabled(true);
        switchHealthCheckEnabled.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            cameraSettingsCopy.setHealthCheckEnabled(isChecked);
        });

        switchUseMotionObject = findViewById(R.id.useMotionObject);
        switchUseMotionObject.setChecked(cameraSettingsCopy.isShowMotionObject());
        switchUseMotionObject.setEnabled(true);
        switchUseMotionObject.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            cameraSettingsCopy.setShowMotionObject(isChecked);
        });

        switchContinuousMonitoring = findViewById(R.id.continuousSwitch);
        switchContinuousMonitoring.setChecked(cameraSettingsCopy.isContinuousMonitoring());
        switchContinuousMonitoring.setEnabled(true);
        switchContinuousMonitoring.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            cameraSettingsCopy.setContinuousMonitoring(isChecked);
        });

        editTextForMotionInterval = findViewById(R.id.editTextForMotionInterval);
        editTextForMotionInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextForMotionInterval.setText(cameraSettingsCopy.getInterval() + "");
        editTextForMotionInterval.setEnabled(true);

        editTextForMotionArea = findViewById(R.id.editTextForMotionArea);
        editTextForMotionArea.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextForMotionArea.setText(cameraSettingsCopy.getMotionArea() + "");
        editTextForMotionArea.setEnabled(true);
        editTextForMotionArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                cameraSettingsCopy.setMotionArea(Integer.parseInt(String.valueOf("".contentEquals(charSequence) ? "0" : charSequence)));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        editTextForNoiseLevel = findViewById(R.id.editTextForNoiseLevel);
        editTextForNoiseLevel.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextForNoiseLevel.setText(cameraSettingsCopy.getNoiseLevel() + "");
        editTextForNoiseLevel.setEnabled(true);
        editTextForNoiseLevel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                cameraSettingsCopy.setNoiseLevel(Integer.parseInt(String.valueOf("".contentEquals(charSequence) ? "0" : charSequence)));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        editTextForRebootTimeout = findViewById(R.id.editTextForRebootDelay);
        editTextForRebootTimeout.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextForRebootTimeout.setText(cameraSettingsCopy.getRebootTimeoutSec() + "");
        editTextForRebootTimeout.setEnabled(true);
        editTextForRebootTimeout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                cameraSettingsCopy.setRebootTimeoutSec(Integer.parseInt(String.valueOf("".contentEquals(charSequence) ? "0" : charSequence)));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        editTextForRecordLength = findViewById(R.id.editTextForRecordLength);
        editTextForRecordLength.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextForRecordLength.setText(cameraSettingsCopy.getRecordLength() + "");
        editTextForRecordLength.setEnabled(true);
        editTextForRecordLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                cameraSettingsCopy.setRecordLength(Integer.parseInt(String.valueOf("".contentEquals(charSequence) ? "0" : charSequence)));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    public void finish() {
        pushChanges();
        super.finish();
    }

    private void pushChanges() {
        if (cameraSettingsChangeId != cameraSettingsCopy.hashCode()) {
            getActiveServerRootReference().child(SETTINGS_ROOT).child(SETTINGS_CAMERA).child(cameraName).setValue(cameraSettingsCopy);
        }
    }
}
