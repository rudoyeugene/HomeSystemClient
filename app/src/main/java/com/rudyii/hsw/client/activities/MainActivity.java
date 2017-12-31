package com.rudyii.hsw.client.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;

public class MainActivity extends AppCompatActivity {
    private Random random = new Random();
    private ToggleButton systemMode, systemState, portsState;
    private TextView armedModeText, armedStateText;
    private boolean buttonsChangedInternally, uiIsActive;
    private MainActivityBroadcastReceiver mainActivityBroadcastReceiver = new MainActivityBroadcastReceiver();
    private Handler serverLastPingHandler, updateDataHandler;
    private Runnable serverLastPingRunnable, updateDataRunnable;
    private long serverLastPing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiIsActive = true;

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
        systemMode.setTextOn(getString(R.string.system_mode_automatic_text));
        systemMode.setTextOff(getString(R.string.system_mode_manual_text));
        systemMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculateSystemStateBasedOn(systemMode, systemState);
            }
        });

        systemState = (ToggleButton) findViewById(R.id.systemState);
        systemState.setTextOn(getString(R.string.system_state_armed_text));
        systemState.setTextOff(getString(R.string.system_state_disarmed_text));
        systemState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculateSystemStateBasedOn(systemMode, systemState);
            }
        });

        updateData();

        buildHandlers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "Main Activity resumed");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(StatusesListener.HSC_STATUSES_UPDATED);

        registerReceiver(mainActivityBroadcastReceiver, intentFilter);

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

        uiIsActive = false;

        serverLastPingHandler.removeCallbacks(serverLastPingRunnable);
        updateDataHandler.removeCallbacks(updateDataRunnable);

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

        switch (itemId) {
            case R.id.settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;

            case R.id.about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                break;
        }

        return true;
    }

    private void openCameraApp() {
        String cameraAppPackageName = getCameraAppPackageName();

        if (cameraAppPackageName == null) {
            new ToastDrawer().showToast(getResources().getString(R.string.choose_camera_app_text));
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (getPackageManager().getLaunchIntentForPackage(cameraAppPackageName) == null) {
            new ToastDrawer().showToast(getResources().getString(R.string.choose_camera_app_error_text));
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
                serverLastPing = (long) info.get("ping");
                Long serverUptime = (long) info.get("uptime");

                TextView serverVersionTextValue = (TextView) findViewById(R.id.serverVersionTextValue);
                serverVersionTextValue.setText(serverVersion);

                TextView serverLastPingTextValue = (TextView) findViewById(R.id.serverLastPingTextValue);
                serverLastPingTextValue.setText(calculatePing(serverLastPing));
                if (System.currentTimeMillis() - serverLastPing > 300000L) {
                    serverLastPingTextValue.setTextColor(getApplicationContext().getColor(R.color.red));
                }

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
                Boolean portsOpen = Boolean.valueOf(state.get("portsOpen").toString());
                HashMap<String, Object> buttonsState = buildDataForMainActivityFrom(armedMode, armedState, portsOpen);

                buttonsChangedInternally = true;
                updateModeStateButtons(buttonsState);
                portsState.setChecked(portsOpen);
                buttonsChangedInternally = false;


                armedModeText = (TextView) findViewById(R.id.systemModeText);
                armedModeText.setText(buttonsState.get("systemModeText").toString());
                if ("auto".equalsIgnoreCase(armedMode)) {
                    armedModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                } else {
                    armedModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                }

                armedStateText = (TextView) findViewById(R.id.systemStateText);
                armedStateText.setText(buttonsState.get("systemStateText").toString());
                if ("armed".equalsIgnoreCase(armedState)) {
                    armedStateText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                } else if ("disarmed".equalsIgnoreCase(armedState)) {
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

    private String calculatePing(long serverLastPing) {
        return getCurrentTimeAndDateDoubleDotsDelimFrom(serverLastPing);
    }

    private String calculateUptime(Long serverUptime) {
        long days = TimeUnit.MILLISECONDS.toDays(serverUptime);
        long hours = TimeUnit.MILLISECONDS.toHours(serverUptime) % 24L;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(serverUptime) % 60L;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(serverUptime) % 60L;

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

    private void buildHandlers() {
        serverLastPingHandler = new Handler();
        updateDataHandler = new Handler();

        serverLastPingRunnable = new Runnable() {
            @Override
            public void run() {
                if (uiIsActive && serverLastPing > 0 && (System.currentTimeMillis() - serverLastPing > 300000)) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    TextView txt = (TextView) findViewById(R.id.serverLastPingTextValue);
                    if (txt.getVisibility() == View.VISIBLE) {
                        txt.setVisibility(View.INVISIBLE);
                    } else {
                        txt.setVisibility(View.VISIBLE);
                    }
                    serverLastPingHandler.postDelayed(this, 1000);
                }
            }
        };

        updateDataRunnable = new Runnable() {
            @Override
            public void run() {
                if (uiIsActive) {
                    buttonsChangedInternally = true;

                    DatabaseReference infoRef = getRootReference().child("/info");
                    infoRef.addListenerForSingleValueEvent(buildInfoValueEventListener());

                    buttonsChangedInternally = false;

                    updateDataHandler.postDelayed(this, 60000);
                }
            }
        };

        serverLastPingHandler.post(serverLastPingRunnable);
        updateDataHandler.post(updateDataRunnable);
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
