package com.rudyii.hsw.client.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.listeners.StatusesListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.rudyii.hsw.client.HomeSystemClientApplication.HSC_SERVER_CHANGED;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServerAlias;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getServersList;
import static com.rudyii.hsw.client.helpers.Utils.retrievePermissions;
import static com.rudyii.hsw.client.helpers.Utils.switchActiveServerTo;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getStringValueFromSettings;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;

public class MainActivity extends AppCompatActivity {
    private Random random = new Random();
    private Switch systemMode, systemState, switchPorts;
    private ImageButton buttonResendHourlyReport, buttonResendWeeklyReport, buttonSystemLog, buttonCameraApp;
    private TextView armedModeText, armedStateText;
    private boolean buttonsChangedInternally;
    private MainActivityBroadcastReceiver mainActivityBroadcastReceiver = new MainActivityBroadcastReceiver();
    private Handler serverLastPingHandler, updateDataHandler;
    private Runnable serverLastPingRunnable, updateDataRunnable;
    private ColorStateList defaultTextColor;
    private long serverLastPing;
    private boolean initComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Main Activity created");

        setContentView(R.layout.activity_main);

        TextView serverLastPingTextValue = (TextView) findViewById(R.id.serverVersionText);
        defaultTextColor = serverLastPingTextValue.getTextColors();

        buttonResendHourlyReport = (ImageButton) findViewById(R.id.buttonResendHourly);
        buttonResendHourlyReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRootReference().child("requests/resendHourly").setValue(random.nextInt(999));
            }
        });
        buttonResendHourlyReport.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new ToastDrawer().showToast(getResources().getString(R.string.text_resend_hourly_text));
                return true;
            }
        });

        buttonResendWeeklyReport = (ImageButton) findViewById(R.id.buttonResendWeekly);
        buttonResendWeeklyReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRootReference().child("requests/resendWeekly").setValue(random.nextInt(999));
            }
        });
        buttonResendWeeklyReport.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new ToastDrawer().showToast(getResources().getString(R.string.text_resend_weekly_text));
                return true;
            }
        });

        buttonSystemLog = (ImageButton) findViewById(R.id.buttonSystemLog);
        buttonSystemLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SystemLogActivity.class));
            }
        });
        buttonSystemLog.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new ToastDrawer().showToast(getResources().getString(R.string.text_system_log_text));
                return true;
            }
        });

        buttonCameraApp = (ImageButton) findViewById(R.id.buttonCameraApp);
        buttonCameraApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCameraApp();
            }
        });
        buttonCameraApp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new ToastDrawer().showToast(getResources().getString(R.string.text_camera_app_text));
                return true;
            }
        });
        refreshCameraAppIcon();

        switchPorts = (Switch) findViewById(R.id.switchPorts);
        switchPorts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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

        systemMode = (Switch) findViewById(R.id.switchSystemMode);
        systemMode.setTextOn(getString(R.string.toggle_button_system_mode_state_automatic_text));
        systemMode.setTextOff(getString(R.string.toggle_button_system_mode_manual_text));
        systemMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculateSystemStateBasedOn(systemMode, systemState);
            }
        });

        systemState = (Switch) findViewById(R.id.switchSystemState);
        systemState.setTextOn(getString(R.string.toggle_button_system_state_armed_text));
        systemState.setTextOff(getString(R.string.toggle_button_system_state_disarmed_text));
        systemState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calculateSystemStateBasedOn(systemMode, systemState);
            }
        });

        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Main Activity resumed");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(StatusesListener.HSC_STATUSES_UPDATED);

        buildServersList();

        updateData();
        buildHandlers();
        refreshCameraAppIcon();
        registerReceiver(mainActivityBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Main Activity paused");

        serverLastPingHandler.removeCallbacks(serverLastPingRunnable);
        updateDataHandler.removeCallbacks(updateDataRunnable);
        unregisterReceiver(mainActivityBroadcastReceiver);
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
            new ToastDrawer().showToast(getResources().getString(R.string.toast_choose_camera_app_text));
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (getPackageManager().getLaunchIntentForPackage(cameraAppPackageName) == null) {
            new ToastDrawer().showToast(getResources().getString(R.string.toast_choose_camera_app_error_text));
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else {
            Intent intent = getPackageManager().getLaunchIntentForPackage(cameraAppPackageName);
            startActivity(intent);
        }
    }

    private void calculateSystemStateBasedOn(Switch systemMode, Switch systemState) {
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
                switchPorts.setChecked(portsOpen);
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
            builder.append(days).append(getResources().getString(R.string.text_day));
        } else if (days > 1) {
            builder.append(days).append(getResources().getString(R.string.text_days));
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
                TextView serverLastPingTextValue = (TextView) findViewById(R.id.serverLastPingTextValue);
                if (serverLastPing > 0 && System.currentTimeMillis() - serverLastPing > 300000) {
                    serverLastPingTextValue.setTextColor(getApplicationContext().getColor(R.color.red));
                    if (serverLastPingTextValue.getVisibility() == View.VISIBLE) {
                        serverLastPingTextValue.setVisibility(View.INVISIBLE);
                    } else {
                        serverLastPingTextValue.setVisibility(View.VISIBLE);
                    }
                } else {
                    serverLastPingTextValue.setTextColor(defaultTextColor);
                    serverLastPingTextValue.setVisibility(View.VISIBLE);
                }
                serverLastPingHandler.postDelayed(this, 1000);
            }
        };

        updateDataRunnable = new Runnable() {
            @Override
            public void run() {
                buttonsChangedInternally = true;

                DatabaseReference infoRef = getRootReference().child("/info");
                infoRef.addListenerForSingleValueEvent(buildInfoValueEventListener());

                buttonsChangedInternally = false;

                updateDataHandler.postDelayed(this, 60000);
            }
        };

        serverLastPingHandler.postDelayed(serverLastPingRunnable, 1000);
        updateDataHandler.post(updateDataRunnable);
    }

    private void buildServersList() {
        Spinner serversList = (Spinner) findViewById(R.id.serverList);
        ArrayAdapter<String> serversArray = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, getServersList());
        serversArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        serversList.setAdapter(serversArray);

        final int[] currentItem = new int[1];
        try {
            currentItem[0] = serversArray.getPosition(getActiveServerAlias());
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, "No paired servers");
        }

        serversList.setSelection(currentItem[0]);
        serversList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View convertView, int selected, long current) {
                String selectedServerName = (String) parent.getItemAtPosition(selected);

                if (currentItem[0] != selected) {
                    currentItem[0] = selected;
                    switchActiveServerTo(selectedServerName);
                    ((TextView) convertView).setText(selectedServerName);
                    updateData();

                    Intent intent = new Intent();
                    intent.setAction(HSC_SERVER_CHANGED);
                    sendBroadcast(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void refreshCameraAppIcon() {
        try {
            Drawable icon = getPackageManager().getApplicationIcon(getStringValueFromSettings("CAMERA_APP"));
            if (icon != null) {
                buttonCameraApp.setImageDrawable(icon);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void requestPermissions() {
        ArrayList<String> permissionsToBeRequested = new ArrayList<>();

        permissionsToBeRequested.addAll(Arrays.asList(retrievePermissions()));

        if (permissionsToBeRequested.size() > 0) {
            String[] permissionsArray = new String[permissionsToBeRequested.size()];
            permissionsToBeRequested.toArray(permissionsArray);
            requestPermissions(permissionsArray, random.nextInt(999));
        }
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

            switchPorts.setChecked((boolean) statusesData.get("portsState"));

            updateModeStateButtons(statusesData);

            buttonsChangedInternally = false;
        }
    }
}
