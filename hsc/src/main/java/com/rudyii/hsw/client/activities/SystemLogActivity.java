package com.rudyii.hsw.client.activities;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.LOG_ROOT;
import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.buildFromPropertiesMap;
import static com.rudyii.hsw.client.helpers.Utils.currentLocale;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getLooper;
import static com.rudyii.hsw.client.helpers.Utils.getSystemModeLocalized;
import static com.rudyii.hsw.client.helpers.Utils.getSystemStateLocalized;
import static com.rudyii.hsw.client.helpers.Utils.readImageFromUrl;
import static com.rudyii.hsw.client.helpers.Utils.saveDataFromUrl;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getActiveServerRootReference;
import static java.util.Collections.sort;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.logs.CameraRebootLog;
import com.rudyii.hs.common.objects.logs.IspLog;
import com.rudyii.hs.common.objects.logs.LogBase;
import com.rudyii.hs.common.objects.logs.MotionLog;
import com.rudyii.hs.common.objects.logs.SimpleWatcherLog;
import com.rudyii.hs.common.objects.logs.StartStopLog;
import com.rudyii.hs.common.objects.logs.StateChangedLog;
import com.rudyii.hs.common.objects.logs.UploadLog;
import com.rudyii.hsw.client.BuildConfig;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.LogItem;
import com.rudyii.hsw.client.helpers.LogListAdapter;
import com.rudyii.hsw.client.helpers.ToastDrawer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Jack on 14.01.2018.
 */

public class SystemLogActivity extends AppCompatActivity {
    public static final String HSC_SYSTEM_LOG_ITEM_CLICKED = "com.rudyii.hsw.client.HSC_SYSTEM_LOG_ITEM_CLICKED";
    private final ArrayList<LogItem> systemLog = new ArrayList<>();
    private final SystemLogBroadcastReceiver systemLogBroadcastReceiver = new SystemLogBroadcastReceiver();
    @SuppressWarnings("FieldCanBeLocal")
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    @SuppressWarnings("FieldCanBeLocal")
    private LinearLayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private DatabaseReference logRef;
    private ValueEventListener logValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "SystemLog Activity created");

        setContentView(R.layout.activity_system_log);
        setTitle(getResources().getString(R.string.label_system_log));

        logRef = getActiveServerRootReference().child(LOG_ROOT);
        logValueEventListener = getValueEventListener();

        logRef.addValueEventListener(logValueEventListener);

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(false);

        mRecyclerView = findViewById(R.id.logRecyclerView);
        mRecyclerView.setHasFixedSize(false);

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(true);
        mRecyclerView.setItemAnimator(itemAnimator);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new LogListAdapter(getApplicationContext(), systemLog);

        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int itemId = viewHolder.getLayoutPosition();
                mAdapter.notifyItemRemoved(itemId);

                LogItem logItem = systemLog.get(itemId);
                logRef.child(logItem.getTimestamp().toString()).removeValue();
                systemLog.remove(logItem);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "System Log Activity paused");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HSC_SYSTEM_LOG_ITEM_CLICKED);

        registerReceiver(systemLogBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "System Log Activity paused");

        unregisterReceiver(systemLogBroadcastReceiver);

        logRef.removeEventListener(logValueEventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.system_log_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.clear_log:
                systemLog.clear();
                mAdapter.notifyDataSetChanged();
                logRef.removeValue();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private ValueEventListener getValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mSwipeRefreshLayout.setRefreshing(true);
                    systemLog.clear();
                    dataSnapshot.getChildren().forEach(logBase -> systemLog.add(buildAndFillLogItem(logBase.getValue())));

                    sort(systemLog, (logItem1, logItem2) -> {
                        if (logItem1.getTimestamp().equals(logItem2.getTimestamp())) {
                            return 0;
                        } else if (logItem1.getTimestamp() > logItem2.getTimestamp()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    });

                    mSwipeRefreshLayout.setRefreshing(false);
                    mAdapter.notifyDataSetChanged();
                } else {
                    new ToastDrawer().showToast(getString(R.string.text_system_log_is_empty));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private LogItem buildAndFillLogItem(Object value) {
        LogBase logBase = buildFromPropertiesMap((Map<String, String>) value, LogBase.class);
        long logRecordId = logBase.getEventId();
        String title = getCurrentTimeAndDateDoubleDotsDelimFrom(logRecordId);
        LogItem logItem = null;
        Bitmap image = null;
        String description = null;

        switch (logBase.getLogType()) {
            case STATE_CHANGED:
                StateChangedLog stateChangedLog = buildFromPropertiesMap((Map<String, String>) value, StateChangedLog.class);

                switch (stateChangedLog.getSystemState()) {
                    case ARMED:
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.shortcut_arm);
                        break;
                    case DISARMED:
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.shortcut_disarm);
                        break;
                    case RESOLVING:
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.shortcut_auto);
                        break;
                    default:
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_warning);
                        break;
                }

                description = String.format(currentLocale, "%s:%s", getSystemModeLocalized(stateChangedLog.getSystemMode()), getSystemStateLocalized(stateChangedLog.getSystemState()));
                break;

            case MOTION_DETECTED:
                MotionLog motionLog = buildFromPropertiesMap((Map<String, String>) value, MotionLog.class);
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_snapshot);
                description = String.format(currentLocale, "%s: %d%%", motionLog.getCameraName(), motionLog.getMotionArea());

                logItem = new LogItem(getApplicationContext()) {
                    @Override
                    public void fireAction() {
                        Handler handler = new Handler(getLooper());
                        handler.post(() -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(motionLog.getImageUrl()));
                            File outputDir = getApplicationContext().getCacheDir();
                            try {
                                File outputFile = File.createTempFile(String.valueOf(motionLog.getEventId()), ".jpg", outputDir);
                                if (outputFile.length() == 0) {
                                    Bitmap bitmap = readImageFromUrl(motionLog.getImageUrl());
                                    FileOutputStream fos = new FileOutputStream(outputFile);
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                    fos.close();
                                }
                                intent = new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", outputFile));
                            } catch (IOException e) {
                                new ToastDrawer().showToast("Failed to load image");
                                e.printStackTrace();
                            } finally {
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                            }
                        });

                    }
                };

                logItem.fill(image, title, description, logRecordId);
                break;

            case RECORD_UPLOADED:
                UploadLog uploadLog = buildFromPropertiesMap((Map<String, String>) value, UploadLog.class);
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_video);
                description = String.format(currentLocale, "%s", uploadLog.getCameraName());

                logItem = new LogItem(getApplicationContext()) {
                    @Override
                    public void fireAction() {
                        Handler handler = new Handler(getLooper());
                        handler.post(() -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uploadLog.getVideoUrl()));
                            File outputDir = getApplicationContext().getCacheDir();
                            try {
                                File outputFile = File.createTempFile(String.valueOf(uploadLog.getEventId()), ".mp4", outputDir);
                                if (outputFile.length() == 0) {
                                    saveDataFromUrl(uploadLog.getVideoUrl(), outputFile);
                                }
                                intent = new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", outputFile));
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                startActivity(intent);
                            }
                        });
                    }
                };

                logItem.fill(image, title, description, logRecordId);
                break;

            case ISP_CHANGED:
                IspLog ispLog = buildFromPropertiesMap((Map<String, String>) value, IspLog.class);
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_isp);
                description = String.format(currentLocale, "%s > %s", ispLog.getIspName(), ispLog.getIspIp());
                break;

            case CAMERA_REBOOTED:
                CameraRebootLog cameraRebootLog = buildFromPropertiesMap((Map<String, String>) value, CameraRebootLog.class);
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_webcam);
                description = String.format(currentLocale, "%s %s", cameraRebootLog.getCameraName(), getResources().getString(R.string.notif_text_camera_is_rebooting));
                break;

            case SERVER_START_STOP:
                StartStopLog startStopLog = buildFromPropertiesMap((Map<String, String>) value, StartStopLog.class);

                switch (startStopLog.getServerState()) {
                    case STARTED:
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_server_started);
                        description = String.format(currentLocale, "%s, PID: %d", startStopLog.getServerState(), startStopLog.getPid());
                        break;

                    case STOPPED:
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_server_stopped);
                        description = startStopLog.getServerState().name();
                        break;
                }
                break;

            case SIMPLE_WATCHER_FIRED:
                SimpleWatcherLog simpleWatcherLog = buildFromPropertiesMap((Map<String, String>) value, SimpleWatcherLog.class);
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_warning);
                description = simpleWatcherLog.getOriginalText();
                break;

        }

        if (logItem == null) {
            return new LogItem(getApplicationContext()).fill(image, title, description, logRecordId);
        } else {
            return logItem;
        }
    }

    private class SystemLogBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int itemId = (int) intent.getSerializableExtra("itemId");
            systemLog.get(itemId).fireAction();
        }
    }
}
