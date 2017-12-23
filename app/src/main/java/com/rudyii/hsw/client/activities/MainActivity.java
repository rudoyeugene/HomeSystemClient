package com.rudyii.hsw.client.activities;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.listeners.StatusesListener;
import com.rudyii.hsw.client.services.FirebaseService;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.buildMainActivityButtonsStateMapFrom;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;

public class MainActivity extends AppCompatActivity {
    private Random random = new Random();
    private ToggleButton systemMode, systemState, portsState;
    private TextView armedModeText, armedStateText;
    private boolean buttonsChangedInternally;
    private MainActivityBroadcastReceiver mainActivityBroadcastReceiver = new MainActivityBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Main Activity created");

        setContentView(R.layout.activity_main);

        Button resendHourlyReport = (Button) findViewById(R.id.resendHourly);
        resendHourlyReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRootReference().child("requests/resendHourly").setValue(random.nextInt(999));
            }
        });

        Button resendHourlyWeekly = (Button) findViewById(R.id.resendWeekly);
        resendHourlyWeekly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRootReference().child("requests/resendWeekly").setValue(random.nextInt(999));
            }
        });

        portsState = (ToggleButton) findViewById(R.id.portsState);
        portsState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonsChangedInternally) {
                    return;
                }

                if (isChecked) {
                    getRootReference().child("requests/portsOpen").setValue(true);
                } else {
                    getRootReference().child("requests/portsOpen").setValue(false);
                }
            }
        });

        Button cameras = (Button) findViewById(R.id.cameras);
        cameras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCameraApp();
            }
        });

        systemMode = (ToggleButton) findViewById(R.id.systemMode);
        systemMode.setTextOn(getString(R.string.SYSTEM_MODE_ON_TEXT));
        systemMode.setTextOff(getString(R.string.SYSTEM_MODE_OFF_TEXT));
        systemMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculateSystemStateBasedOn(systemMode, systemState);
            }
        });

        systemState = (ToggleButton) findViewById(R.id.systemState);
        systemState.setTextOn(getString(R.string.SYSTEM_STATE_ON_TEXT));
        systemState.setTextOff(getString(R.string.SYSTEM_STATE_OFF_TEXT));
        systemState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculateSystemStateBasedOn(systemMode, systemState);
            }
        });

        updateData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "Main Activity resumed");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(StatusesListener.HSC_STATUSES_UPDATED);

        registerReceiver(mainActivityBroadcastReceiver, intentFilter);

        if (isMyServiceRunning(FirebaseService.class)) {
            stopService(new Intent(getApplicationContext(), FirebaseService.class));
            startService(new Intent(getApplicationContext(), FirebaseService.class));
        } else {
            startService(new Intent(getApplicationContext(), FirebaseService.class));
        }

        updateData();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "Main Activity stopped");

        unregisterReceiver(mainActivityBroadcastReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i(TAG, "Main Activity paused");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }

        return true;
    }

    private void openCameraApp() {
        String cameraAppPackageName = getCameraAppPackageName();

        if (cameraAppPackageName == null) {
            new ToastDrawer().showToast("Camera application is not selected", "Select new Camera application");
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (getPackageManager().getLaunchIntentForPackage(cameraAppPackageName) == null) {
            new ToastDrawer().showToast("Camera application uninstalled", "Select new Camera application");
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else {
            Intent intent = getPackageManager().getLaunchIntentForPackage(cameraAppPackageName);
            startActivity(intent);
        }
    }

    private void calculateSystemStateBasedOn(ToggleButton systemMode, ToggleButton systemState) {
        Map<String, String> stateRequest = new HashMap<>();

        if (buttonsChangedInternally) {
            return;
        }

        if (systemMode.isChecked()) {
            systemState.setChecked(false);
            systemState.setEnabled(false);

            stateRequest.put("armedMode", "AUTOMATIC");
            stateRequest.put("armedState", "AUTO");

            getRootReference().child("requests/state").setValue(stateRequest);

        } else if (!systemMode.isChecked() && systemState.isChecked()) {
            systemMode.setChecked(false);
            systemState.setEnabled(true);

            stateRequest.put("armedMode", "MANUAL");
            stateRequest.put("armedState", "ARMED");

            getRootReference().child("requests/state").setValue(stateRequest);

        } else if (!systemMode.isChecked() && !systemState.isChecked()) {
            systemMode.setChecked(false);
            systemState.setEnabled(true);

            stateRequest.put("armedMode", "MANUAL");
            stateRequest.put("armedState", "DISARMED");

            getRootReference().child("requests/state").setValue(stateRequest);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void updateData() {
        buttonsChangedInternally = true;

        DatabaseReference infoRef = getRootReference().child("/info");
        infoRef.addListenerForSingleValueEvent(buildInfoValueEventListener());

        DatabaseReference statusesRef = getRootReference().child("/statuses");
        statusesRef.addListenerForSingleValueEvent(buildStatusesValueEventListener());

        buttonsChangedInternally = false;
    }

    private ValueEventListener buildInfoValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> info = (Map<String, Object>) dataSnapshot.getValue();

                if (info == null) {
                    return;
                }

                String serverVersion = info.get("serverVersion").toString();
                String serverLastPing = info.get("ping").toString();
                String serverUptime = info.get("uptime").toString();

                TextView serverVersionTextValue = (TextView) findViewById(R.id.serverVersionTextValue);
                serverVersionTextValue.setText(serverVersion);

                TextView serverLastPingTextValue = (TextView) findViewById(R.id.serverLastPingTextValue);
                serverLastPingTextValue.setText(calculatePing(serverLastPing));

                TextView serverUptimeTextValue = (TextView) findViewById(R.id.serverUptimeTextValue);
                serverUptimeTextValue.setText(calculateUptime(serverUptime));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private ValueEventListener buildStatusesValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> state = (Map<String, Object>) dataSnapshot.getValue();

                if (state == null) {
                    return;
                }

                String armedMode = state.get("armedMode").toString();
                String armedState = state.get("armedState").toString();
                HashMap<String, Object> buttonsState = buildMainActivityButtonsStateMapFrom(armedMode, armedState);

                buttonsChangedInternally = true;
                updateModeStateButtons(buttonsState);
                buttonsChangedInternally = false;

                Boolean portsOpen = Boolean.valueOf(state.get("portsOpen").toString());
                portsState.setChecked(portsOpen);

                armedModeText = (TextView) findViewById(R.id.systemModeText);
                armedModeText.setText(armedMode);
                if (armedMode.equalsIgnoreCase("auto")) {
                    armedModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                } else {
                    armedModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                }

                armedStateText = (TextView) findViewById(R.id.systemStateText);
                armedStateText.setText(armedState);
                if (armedState.equalsIgnoreCase("armed")) {
                    armedStateText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                } else if (armedState.equalsIgnoreCase("disarmed")) {
                    armedStateText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                } else {
                    armedStateText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private String calculatePing(String serverLastPing) {
        Long pingTimestamp = Long.decode(serverLastPing);

        return getCurrentTimeAndDateDoubleDotsDelimFrom(pingTimestamp);
    }

    private String calculateUptime(String serverUptime) {
        Long uptime = Long.decode(serverUptime);
        long days = TimeUnit.MILLISECONDS.toDays(uptime);
        long hours = TimeUnit.MILLISECONDS.toHours(uptime) % 24L;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60L;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) % 60L;

        StringBuilder builder = new StringBuilder();

        if (days == 1) {
            builder.append((String.format("%s Day ", days)));
        } else if (days > 1) {
            builder.append((String.format("%s Days ", days)));
        }

        if (hours < 10) {
            builder.append((String.format("0%s:", hours)));
        } else {
            builder.append((String.format("%s:", hours)));
        }

        if (minutes < 10) {
            builder.append((String.format("0%s:", minutes)));
        } else {
            builder.append((String.format("%s:", minutes)));
        }

        if (seconds < 10) {
            builder.append((String.format("0%s", seconds)));
        } else {
            builder.append(String.format("%s", seconds));
        }

        return builder.toString();
    }

    private String getCameraAppPackageName() {
        return getStringValueFromSettings("CAMERA_APP");
    }

    private void updateModeStateButtons(HashMap<String, Object> statusesData) {
        systemMode.setChecked((boolean) statusesData.get("systemModeChecked"));

        systemState.setChecked((boolean) statusesData.get("systemStateChecked"));
        systemState.setEnabled((boolean) statusesData.get("systemStateEnabled"));
    }

    public class MainActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateData();

            buttonsChangedInternally = true;

            HashMap<String, Object> statusesData = (HashMap<String, Object>) intent.getSerializableExtra("HSC_STATUSES_UPDATED");

            if (statusesData == null) {
                return;
            }

            armedModeText.setText((String) statusesData.get("systemModeText"));
            armedModeText.setTextColor((int) statusesData.get("systemModeTextColor"));

            armedStateText.setText((String) statusesData.get("systemStateText"));
            armedStateText.setTextColor((int) statusesData.get("systemStateTextColor"));

            portsState = (ToggleButton) findViewById(R.id.portsState);
            portsState.setChecked((boolean) statusesData.get("portsState"));

            updateModeStateButtons(statusesData);

            buttonsChangedInternally = false;
        }
    }
}
