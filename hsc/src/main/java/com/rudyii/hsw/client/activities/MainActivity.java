package com.rudyii.hsw.client.activities;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.INFO_PING;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.INFO_ROOT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.INFO_SERVER;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.LOG_ROOT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.REQUEST_HOURLY_REPORT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.REQUEST_ROOT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.REQUEST_SYSTEM_MODE_AND_STATE;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.STATUS_ROOT;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.STATUS_SERVER;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.USAGE_STATS_ROOT;
import static com.rudyii.hs.common.type.NotificationType.ALL;
import static com.rudyii.hs.common.type.NotificationType.MOTION_DETECTED;
import static com.rudyii.hs.common.type.NotificationType.VIDEO_RECORDED;
import static com.rudyii.hs.common.type.SystemModeType.AUTOMATIC;
import static com.rudyii.hs.common.type.SystemModeType.MANUAL;
import static com.rudyii.hs.common.type.SystemStateType.ARMED;
import static com.rudyii.hs.common.type.SystemStateType.DISARMED;
import static com.rudyii.hsw.client.BuildConfig.COMPATIBLE_SERVER_VERSION;
import static com.rudyii.hsw.client.BuildConfig.SERVER_DOWNLOAD_URL;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.NotificationChannelsBuilder.NOTIFICATION_CHANNEL_MUTED;
import static com.rudyii.hsw.client.helpers.Utils.DELAYED_ARM_DELAY_SECS;
import static com.rudyii.hsw.client.helpers.Utils.calculateUptimeFromMinutes;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.getActiveServer;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getLooper;
import static com.rudyii.hsw.client.helpers.Utils.getPrimaryAccountEmail;
import static com.rudyii.hsw.client.helpers.Utils.getSystemModeLocalized;
import static com.rudyii.hsw.client.helpers.Utils.getSystemStateLocalized;
import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServer;
import static com.rudyii.hsw.client.helpers.Utils.retrievePermissions;
import static com.rudyii.hsw.client.helpers.Utils.systemIsOnDarkMode;
import static com.rudyii.hsw.client.helpers.Utils.updateServer;
import static com.rudyii.hsw.client.providers.DatabaseProvider.addOrUpdateServer;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getIntValueFromSettingsStorage;
import static com.rudyii.hsw.client.providers.DatabaseProvider.setOrUpdateActiveServer;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getActiveServerRootReference;
import static java.util.Objects.requireNonNull;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.ServerStatusChangeRequest;
import com.rudyii.hs.common.objects.info.ServerInfo;
import com.rudyii.hs.common.objects.info.ServerStatus;
import com.rudyii.hs.common.objects.info.Uptime;
import com.rudyii.hs.common.type.NotificationType;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.objects.internal.ServerData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private final Random random = new Random();
    private SwitchCompat systemMode, systemState;
    private ImageButton buttonResendHourlyReport;
    private ImageButton buttonNotificationType;
    private TextView systemModeText, systemStateText;
    private Button serversList;
    private boolean buttonNotificationTypeMuted, buttonResendHourlyEnabled, delayedArmInProgress;
    private Handler serverLastPingHandler;
    private Runnable serverLastPingRunnable;
    private ColorStateList defaultTextColor;
    private DatabaseReference infoRef, statusesRef, pingRef;
    private ValueEventListener infoValueEventListener, statusesValueEventListener, pingValueEventListener;
    private long serverLastPing;
    private boolean systemIsInDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        systemIsInDarkMode = systemIsOnDarkMode();
        setContentView(R.layout.activity_main);
        requestPermissions();

        TextView serverLastPingTextValue = findViewById(R.id.textViewServerVersion);
        defaultTextColor = serverLastPingTextValue.getTextColors();

        serversList = findViewById(R.id.buttonServerList);
        updateServerListButtonActiveServerName();

        buttonResendHourlyReport = findViewById(R.id.buttonResendHourly);
        if (systemIsInDarkMode) {
            buttonResendHourlyReport.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_hourly_inverted));
        }
        resolveHourlyReportIcon();
        buttonResendHourlyReport.setOnClickListener(v -> {
            if (buttonResendHourlyEnabled) {
                new ToastDrawer().showToast(getResources().getString(R.string.text_resend_hourly_request_muted));
            } else {
                getActiveServerRootReference().child(REQUEST_ROOT).child(REQUEST_HOURLY_REPORT).setValue(random.nextInt());
                new ToastDrawer().showToast(getResources().getString(R.string.text_resend_hourly_request_text));
            }
        });
        buttonResendHourlyReport.setOnLongClickListener(v -> {
            muteUnmuteHourlyReporting();
            return true;
        });

        ImageButton buttonUsageStats = findViewById(R.id.buttonUsageChart);
        if (systemIsInDarkMode) {
            buttonUsageStats.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_chart_inverted));
        }
        buttonUsageStats.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), UsageChartActivity.class)));
        buttonUsageStats.setOnLongClickListener(v -> {
            AlertDialog.Builder cleanupLog = new AlertDialog.Builder(MainActivity.this);
            cleanupLog.setTitle(getResources().getString(R.string.dialog_cleanup_usage_stats_title));
            cleanupLog.setMessage(getResources().getString(R.string.dialog_are_you_sure_cant_undo_alert_message));

            cleanupLog.setPositiveButton(getResources().getString(R.string.dialog_yes), (dialogInterface, i) -> getActiveServerRootReference().child(USAGE_STATS_ROOT).removeValue());

            cleanupLog.setNegativeButton(getResources().getString(R.string.dialog_no), (dialogInterface, i) -> {

            });

            cleanupLog.show();

            return true;
        });

        ImageButton buttonSystemLog = findViewById(R.id.buttonSystemLog);
        if (systemIsInDarkMode) {
            buttonSystemLog.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_log_inverted));
        }
        buttonSystemLog.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SystemLogActivity.class)));
        buttonSystemLog.setOnLongClickListener(v -> {
            AlertDialog.Builder cleanupLog = new AlertDialog.Builder(MainActivity.this);
            cleanupLog.setTitle(getResources().getString(R.string.dialog_cleanup_log_title));
            cleanupLog.setMessage(getResources().getString(R.string.dialog_are_you_sure_cant_undo_alert_message));

            cleanupLog.setPositiveButton(getResources().getString(R.string.dialog_yes), (dialogInterface, i) -> getActiveServerRootReference().child(LOG_ROOT).removeValue());

            cleanupLog.setNegativeButton(getResources().getString(R.string.dialog_no), (dialogInterface, i) -> {

            });

            cleanupLog.show();

            return true;
        });

        buttonNotificationType = findViewById(R.id.buttonNotificationType);
        resolveNotificationType();
        buttonNotificationType.setOnClickListener(v -> {
            if (buttonNotificationTypeMuted) {
                new ToastDrawer().showToast(getResources().getString(R.string.text_toast_notification_muted));
            } else {
                switchNotificationTypes();
                drawToastWithNotificationTypeInfo();
            }
        });
        buttonNotificationType.setOnLongClickListener(v -> {
            muteUnmuteButtonNotificationType();
            return true;
        });

        systemMode = findViewById(R.id.switchSystemMode);
        systemMode.setTextOn(getString(R.string.toggle_button_text_system_mode_automatic));
        systemMode.setTextOff(getString(R.string.toggle_button_text_system_mode_manual));
        systemMode.setOnClickListener((buttonView) -> pushSystemStateUpdate());

        systemState = findViewById(R.id.switchSystemState);
        systemState.setTextOn(getString(R.string.toggle_button_text_system_state_armed));
        systemState.setTextOff(getString(R.string.toggle_button_text_system_state_disarmed));
        systemState.setOnClickListener((buttonView) -> pushSystemStateUpdate());

        Button armLater = findViewById(R.id.buttonDelayedArm);
        armLater.setOnClickListener(v -> {
            if (systemMode.isChecked()) {
                new ToastDrawer().showLongToast(getString(R.string.text_toast_automatic_mode_selected));
            } else if (delayedArmInProgress) {
                new ToastDrawer().showLongToast(getString(R.string.text_toast_delayed_arm_in_progress));
            } else {
                delayedArmInProgress = true;

                AsyncTask.execute(() -> {
                    int notificationId = new Random().nextInt();
                    Context context = getAppContext();
                    int delayedArmSeconds = getIntValueFromSettingsStorage(DELAYED_ARM_DELAY_SECS);
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_MUTED)
                            .setSmallIcon(R.drawable.ic_stat_notification)
                            .setContentTitle(String.format(currentLocale, "%s %d %s",
                                    getString(R.string.notif_text_delayed_arm),
                                    delayedArmSeconds,
                                    getString(R.string.text_seconds)))
                            .setProgress(delayedArmSeconds, 0, false);

                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    requireNonNull(mNotificationManager).notify(notificationId, mBuilder.build());

                    int countUp = 1;
                    while (countUp < delayedArmSeconds) {
                        try {
                            Thread.sleep(1000L);
                            int percent = countUp * 100 / delayedArmSeconds;
                            mBuilder.setOngoing(true)
                                    .setContentInfo(percent + "%")
                                    .setProgress(100, percent, false);
                            requireNonNull(mNotificationManager).notify(notificationId, mBuilder.build());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        countUp++;
                    }

                    Handler handler = new Handler(getLooper());
                    handler.post(() -> {
                        systemState.setChecked(true);
                        systemMode.setChecked(true);
                        pushSystemStateUpdate();
                        mNotificationManager.cancel(notificationId);
                        delayedArmInProgress = false;
                    });
                });
            }
        });
        resolveHourlyReportIcon();
    }

    @Override
    protected void onStart() {
        super.onStart();

        subscribeFirebaseListeners();

        buildServersButton();

        updateServerListButtonActiveServerName();
        resolveHourlyReportIcon();
        resolveNotificationType();

        buildHandlers();
    }

    @Override
    protected void onStop() {
        super.onStop();

        serverLastPingHandler.removeCallbacks(serverLastPingRunnable);
        serverLastPingHandler = null;
        serverLastPingRunnable = null;

        unsubscribeFirebaseListeners();
    }

    private void updateServerListButtonActiveServerName() {
        ServerData activeServer = getActiveServer();
        if (activeServer != null) {
            serversList.setText(activeServer.getServerAlias());
            serversList.setTransformationMethod(null);
        }
    }

    private void muteUnmuteButtonNotificationType() {
        ServerData activeServer = getActiveServer();
        activeServer.setNotificationsMuted(!activeServer.isNotificationsMuted());

        buttonNotificationTypeMuted = activeServer.isNotificationsMuted();

        resolveNotificationType();
        addOrUpdateServer(activeServer);
        setOrUpdateActiveServer(activeServer);
        registerUserDataOnServer(activeServer);
        resolveNotificationType();
    }

    private void muteUnmuteHourlyReporting() {
        ServerData activeServer = getActiveServer();
        buttonResendHourlyEnabled = activeServer.isHourlyReportEnabled();
        Drawable icon;

        if (buttonResendHourlyEnabled) {
            buttonResendHourlyEnabled = false;
            if (systemIsInDarkMode) {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_hourly_inverted);
            } else {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_hourly);
            }
        } else {
            buttonResendHourlyEnabled = true;
            if (systemIsInDarkMode) {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted_inverted);
            } else {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted);
            }
        }

        buttonResendHourlyReport.setImageDrawable(icon);
        activeServer.setHourlyReportEnabled(buttonResendHourlyEnabled);
        addOrUpdateServer(activeServer);
        setOrUpdateActiveServer(activeServer);
        registerUserDataOnServer(getActiveServer());
    }

    private void subscribeFirebaseListeners() {
        if (infoRef == null) {
            infoRef = getActiveServerRootReference().child(INFO_ROOT).child(INFO_SERVER);
            infoValueEventListener = buildInfoValueEventListener();
            infoRef.addValueEventListener(infoValueEventListener);
        }

        if (pingRef == null) {
            pingRef = getActiveServerRootReference().child(INFO_ROOT).child(INFO_PING);
            pingValueEventListener = buildPingValueEventListener();
            pingRef.addValueEventListener(pingValueEventListener);
        }

        if (statusesRef == null) {
            statusesRef = getActiveServerRootReference().child(STATUS_ROOT).child(STATUS_SERVER);
            statusesValueEventListener = buildStatusesValueEventListener();
            statusesRef.addValueEventListener(statusesValueEventListener);
        }
    }

    private void unsubscribeFirebaseListeners() {
        infoRef.removeEventListener(infoValueEventListener);
        infoRef = null;
        pingRef.removeEventListener(pingValueEventListener);
        pingRef = null;
        statusesRef.removeEventListener(statusesValueEventListener);
        statusesRef = null;
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

            case R.id.download_server:
                Uri serverUrl = Uri.parse(SERVER_DOWNLOAD_URL);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(serverUrl);
                startActivity(intent);
                break;

            case R.id.share_server:
                startActivity(new Intent(getApplicationContext(), ServerSharingActivity.class));
                break;

            case R.id.about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                break;
        }

        return true;
    }

    private void switchNotificationTypes() {
        if (buttonNotificationTypeMuted) {
            return;
        }

        ServerData activeServer = getActiveServer();
        NotificationType notificationType = activeServer.getNotificationType();
        Drawable icon;

        switch (notificationType) {
            case MOTION_DETECTED:
                if (systemIsInDarkMode) {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_video_recorded_inverted);
                } else {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_video_recorded);
                }
                activeServer.setNotificationType(VIDEO_RECORDED);
                updateServer(activeServer);
                break;

            case VIDEO_RECORDED:
                if (systemIsInDarkMode) {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion_and_video_recorded_inverted);
                } else {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion_and_video_recorded);
                }
                activeServer.setNotificationType(ALL);
                updateServer(activeServer);
                break;

            case ALL:
                if (systemIsInDarkMode) {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion_inverted);
                } else {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion);
                }
                activeServer.setNotificationType(MOTION_DETECTED);
                updateServer(activeServer);
                break;
            default:
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.image_warning);
                break;
        }

        buttonNotificationType.setImageDrawable(icon);
        addOrUpdateServer(activeServer);
        setOrUpdateActiveServer(activeServer);
        registerUserDataOnServer(activeServer);
    }

    private void resolveHourlyReportIcon() {
        ServerData activeServer = getActiveServer();
        buttonResendHourlyEnabled = activeServer != null && activeServer.isHourlyReportEnabled();
        Drawable icon;

        if (buttonResendHourlyEnabled) {
            if (systemIsInDarkMode) {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_hourly_inverted);
            } else {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_hourly);
            }
        } else {
            if (systemIsInDarkMode) {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted_inverted);
            } else {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted);
            }
        }

        buttonResendHourlyReport.setImageDrawable(icon);
    }

    private void resolveNotificationType() {
        ServerData activeServer = getActiveServer();
        NotificationType notificationType = activeServer == null ? ALL : activeServer.getNotificationType();
        buttonNotificationTypeMuted = activeServer != null && activeServer.isNotificationsMuted();
        Drawable icon;

        switch (notificationType) {
            case MOTION_DETECTED:
                if (systemIsInDarkMode) {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion_inverted);
                } else {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion);
                }
                break;

            case VIDEO_RECORDED:
                if (systemIsInDarkMode) {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_video_recorded_inverted);
                } else {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_video_recorded);
                }
                break;

            case ALL:
                if (systemIsInDarkMode) {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion_and_video_recorded_inverted);
                } else {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_on_motion_and_video_recorded);
                }
                break;

            case NONE:
                if (systemIsInDarkMode) {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted_inverted);
                } else {
                    icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted);
                }
                buttonNotificationTypeMuted = true;
                break;

            default:
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.image_warning);
                break;
        }

        if (buttonNotificationTypeMuted) {
            if (systemIsInDarkMode) {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted_inverted);
            } else {
                icon = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.button_muted);
            }
        }

        buttonNotificationType.setImageDrawable(icon);
    }

    private void drawToastWithNotificationTypeInfo() {
        ServerData activeServer = getActiveServer();
        NotificationType notificationType = activeServer.getNotificationType();

        switch (notificationType) {
            case MOTION_DETECTED:
                new ToastDrawer().showLongToast(getResources().getString(R.string.text_toast_notification_type_motion_detected));
                break;

            case VIDEO_RECORDED:
                new ToastDrawer().showLongToast(getResources().getString(R.string.text_toast_notification_type_video_recorded));
                break;

            case ALL:
                new ToastDrawer().showLongToast(getResources().getString(R.string.text_toast_notification_type_both));
                break;
        }
    }

    private void pushSystemStateUpdate() {
        getActiveServerRootReference().child(REQUEST_ROOT).child(REQUEST_SYSTEM_MODE_AND_STATE).setValue(ServerStatusChangeRequest.builder()
                .by(getPrimaryAccountEmail())
                .systemMode(systemMode.isChecked() ? AUTOMATIC : MANUAL)
                .systemState(systemState.isChecked() ? ARMED : DISARMED)
                .build());
    }

    private ValueEventListener buildInfoValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ServerInfo serverInfo = dataSnapshot.getValue(ServerInfo.class);

                    TextView serverVersionTextValue = findViewById(R.id.textViewServerVersionValue);
                    if (COMPATIBLE_SERVER_VERSION.hashCode() > serverInfo.getServerVersion().hashCode()) {
                        serverVersionTextValue.setTextColor(getApplicationContext().getColor(R.color.red));
                    } else {
                        serverVersionTextValue.setTextColor(defaultTextColor);
                    }
                    serverVersionTextValue.setText(serverInfo.getServerVersion());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private ValueEventListener buildPingValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Uptime uptime = dataSnapshot.getValue(Uptime.class);
                    serverLastPing = uptime.getPing();

                    TextView serverLastPingTextValue = findViewById(R.id.textViewServerLastPingValue);
                    serverLastPingTextValue.setText(calculatePing(uptime.getPing()));

                    TextView serverUptimeTextValue = findViewById(R.id.textViewServerUptimeValue);
                    serverUptimeTextValue.setText(calculateUptimeFromMinutes(uptime.getUptime()));

                    TextView usageStatsCurrentSessionTextView = findViewById(R.id.usageStatsCurrentSessionValue);
                    usageStatsCurrentSessionTextView.setText(calculateUptimeFromMinutes(uptime.getCurrentSession()));
                }
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
                if (dataSnapshot.exists()) {
                    ServerStatus serverStatus = dataSnapshot.getValue(ServerStatus.class);

                    systemModeText = findViewById(R.id.textViewForSwitchSystemMode);
                    systemModeText.setText(getSystemModeLocalized(serverStatus.getSystemMode()));
                    if (AUTOMATIC.equals(serverStatus.getSystemMode())) {
                        systemModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                        systemMode.setChecked(true);
                        systemState.setEnabled(false);
                    } else {
                        systemMode.setChecked(false);
                        systemModeText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                        systemState.setEnabled(true);
                    }

                    systemStateText = findViewById(R.id.textViewForSwitchSystemState);
                    systemStateText.setText(getSystemStateLocalized(serverStatus.getSystemState()));
                    if (ARMED.equals(serverStatus.getSystemState())) {
                        systemState.setChecked(true);
                        systemStateText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                    } else if (DISARMED.equals(serverStatus.getSystemState())) {
                        systemState.setChecked(false);
                        systemStateText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                    } else {
                        systemState.setChecked(false);
                        systemStateText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                    }
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

    private void buildHandlers() {
        serverLastPingHandler = new Handler();
        serverLastPingRunnable = new Runnable() {
            @Override
            public void run() {
                TextView serverLastPingTextValue = findViewById(R.id.textViewServerLastPingValue);
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

        serverLastPingHandler.postDelayed(serverLastPingRunnable, 1000);
    }

    private void buildServersButton() {
        serversList.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), SelectServerActivity.class));
        });
    }

    private void requestPermissions() {

        ArrayList<String> permissionsToBeRequested = new ArrayList<>(Arrays.asList(retrievePermissions()));

        if (permissionsToBeRequested.size() > 0) {
            String[] permissionsArray = new String[permissionsToBeRequested.size()];
            permissionsToBeRequested.toArray(permissionsArray);
            requestPermissions(permissionsArray, random.nextInt(999));
        }
    }
}
