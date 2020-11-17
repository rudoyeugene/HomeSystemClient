package com.rudyii.hsw.client.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.rudyii.hsw.client.R;

public class CameraSettingsActivity extends AppCompatActivity {
    public static String HEALTH_CHECK_ENABLED = "healthCheckEnabled";
    public static String USE_MOTION_OBJECT = "useMotionObject";
    public static String INTERVAL = "interval";
    public static String MOTION_AREA = "motionArea";
    public static String NOISE_LEVEL = "noiseLevel";
    public static String REBOOT_TIMEOUT = "rebootTimeout";
    private Intent intent;
    private SwitchCompat switchHealthCheckEnabled, switchUseMotionObject;
    private boolean isHealthCheckEnabled, isUseMotionObject;
    private EditText editTextForMotionInterval, editTextForMotionArea, editTextForNoiseLevel, editTextForRebootTimeout;
    private String cameraName;
    private Long motionInterval, motionArea, noiseLevel, rebootTimeout;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera_settings);

        intent = getIntent();

        cameraName = intent.getStringExtra("cameraName");
        this.setTitle(cameraName);

        switchHealthCheckEnabled = findViewById(R.id.healthCheckEnabled);
        this.isHealthCheckEnabled = Boolean.parseBoolean(intent.getStringExtra(HEALTH_CHECK_ENABLED));
        switchHealthCheckEnabled.setChecked(isHealthCheckEnabled);
        switchHealthCheckEnabled.setEnabled(true);

        switchUseMotionObject = findViewById(R.id.useMotionObject);
        this.isUseMotionObject = Boolean.parseBoolean(intent.getStringExtra(USE_MOTION_OBJECT));
        switchUseMotionObject.setChecked(isUseMotionObject);
        switchUseMotionObject.setEnabled(true);

        editTextForMotionInterval = findViewById(R.id.editTextForMotionInterval);
        this.motionInterval = Long.valueOf(intent.getStringExtra(INTERVAL));
        editTextForMotionInterval.setText(motionInterval.toString());
        editTextForMotionInterval.setEnabled(true);

        editTextForMotionArea = findViewById(R.id.editTextForMotionArea);
        this.motionArea = Long.valueOf(intent.getStringExtra(MOTION_AREA));
        editTextForMotionArea.setText(motionArea.toString());
        editTextForMotionArea.setEnabled(true);

        editTextForNoiseLevel = findViewById(R.id.editTextForNoiseLevel);
        this.noiseLevel = Long.valueOf(intent.getStringExtra(NOISE_LEVEL));
        editTextForNoiseLevel.setText(noiseLevel.toString());
        editTextForNoiseLevel.setEnabled(true);

        editTextForRebootTimeout = findViewById(R.id.editTextForRebootDelay);
        this.rebootTimeout = Long.valueOf(intent.getStringExtra(REBOOT_TIMEOUT));
        editTextForRebootTimeout.setText(rebootTimeout.toString());
        editTextForRebootTimeout.setEnabled(true);
    }

    @Override
    public void finish() {
        verifyChanges();
        super.finish();
    }

    private void verifyChanges() {
        this.isHealthCheckEnabled = switchHealthCheckEnabled.isChecked();
        this.isUseMotionObject = switchUseMotionObject.isChecked();
        this.motionInterval = Long.valueOf(editTextForMotionInterval.getText().toString());
        this.motionArea = Long.valueOf(editTextForMotionArea.getText().toString());
        this.noiseLevel = Long.valueOf(editTextForNoiseLevel.getText().toString());
        this.rebootTimeout = Long.valueOf(editTextForRebootTimeout.getText().toString());

        if (isHealthCheckEnabled != Boolean.parseBoolean(intent.getStringExtra(HEALTH_CHECK_ENABLED))) {
            setIntent();
        } else if (isUseMotionObject != Boolean.parseBoolean(intent.getStringExtra(USE_MOTION_OBJECT))) {
            setIntent();
        } else if (!motionInterval.equals(Long.valueOf(intent.getStringExtra(INTERVAL)))) {
            setIntent();
        } else if (!motionArea.equals(Long.valueOf(intent.getStringExtra(MOTION_AREA)))) {
            setIntent();
        } else if (!noiseLevel.equals(Long.valueOf(intent.getStringExtra(NOISE_LEVEL)))) {
            setIntent();
        } else if (!rebootTimeout.equals(Long.valueOf(intent.getStringExtra(REBOOT_TIMEOUT)))) {
            setIntent();
        }
    }

    private void setIntent() {
        Intent output = new Intent();

        output.putExtra("cameraName", cameraName);
        output.putExtra(HEALTH_CHECK_ENABLED, isHealthCheckEnabled);
        output.putExtra(USE_MOTION_OBJECT, isUseMotionObject);
        output.putExtra(INTERVAL, motionInterval);
        output.putExtra(MOTION_AREA, motionArea);
        output.putExtra(NOISE_LEVEL, noiseLevel);
        output.putExtra(REBOOT_TIMEOUT, rebootTimeout);

        setResult(RESULT_OK, output);
    }
}
