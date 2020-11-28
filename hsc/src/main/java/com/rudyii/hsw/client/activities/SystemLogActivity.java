package com.rudyii.hsw.client.activities;

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
import com.rudyii.hsw.client.BuildConfig;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.LogItem;
import com.rudyii.hsw.client.helpers.LogListAdapter;
import com.rudyii.hsw.client.helpers.ToastDrawer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getLooper;
import static com.rudyii.hsw.client.helpers.Utils.readImageFromUrl;
import static com.rudyii.hsw.client.helpers.Utils.saveDataFromUrl;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;
import static java.util.Collections.sort;

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

        logRef = getRootReference().child("/log");
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
                @SuppressWarnings("unchecked") final HashMap<String, Map<String, Object>> logMap = (HashMap<String, Map<String, Object>>) dataSnapshot.getValue();

                if (logMap == null) {
                    new ToastDrawer().showToast(getString(R.string.text_system_log_is_empty));
                    return;
                }

                mSwipeRefreshLayout.setRefreshing(true);
                systemLog.clear();

                for (Map.Entry<String, Map<String, Object>> entry : logMap.entrySet()) {
                    systemLog.add(buildAndFillLogItem(Long.valueOf(entry.getKey()), entry.getValue()));
                }

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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private LogItem buildAndFillLogItem(Long logRecordId, Map<String, Object> logRecordData) {
        String reason = (String) logRecordData.get("reason");
        String title = getCurrentTimeAndDateDoubleDotsDelimFrom(logRecordId);
        String cameraName = (String) logRecordData.get("cameraName");
        LogItem logItem = null;
        Bitmap image = null;
        String description = null;

        switch (reason) {
            case "systemStateChanged":
                String armedMode = (String) logRecordData.get("armedMode");
                String armedState = (String) logRecordData.get("armedState");

                HashMap<String, Object> statusesData = buildDataForMainActivityFrom(armedMode, armedState);

                switch (armedState) {
                    case "ARMED":
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.shortcut_arm);
                        break;
                    case "DISARMED":
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.shortcut_disarm);
                        break;
                    case "AUTO":
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.shortcut_auto);
                        break;
                    default:
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_warning);
                        break;
                }

                description = getResources().getString(R.string.notif_text_system_state_is)
                        + statusesData.get("systemModeText")
                        + ":" + statusesData.get("systemStateText");
                break;

            case "motionDetected":
                final String[] imageUrl = {(String) logRecordData.get("imageUrl")};
                Long motionArea = (Long) logRecordData.get("motionArea");

                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_snapshot);
                description = cameraName + ": " + motionArea + "%";

                logItem = new LogItem(getApplicationContext()) {
                    @Override
                    public void fireAction() {
                        Handler handler = new Handler(getLooper());
                        handler.post(() -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl[0]));
                            File outputDir = getApplicationContext().getCacheDir();
                            try {
                                File outputFile = File.createTempFile(logRecordId.toString(), ".jpg", outputDir);
                                if (outputFile.length() == 0) {
                                    Bitmap bitmap = readImageFromUrl(imageUrl[0]);
                                    FileOutputStream fos = new FileOutputStream(outputFile);
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                    fos.close();
                                }
                                intent = new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", outputFile));
                            } catch (IOException e) {
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

            case "videoRecorded":
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_video);
                description = cameraName + ": " + logRecordData.get("fileName").toString();
                final String[] videoUrl = {(String) logRecordData.get("videoUrl")};

                logItem = new LogItem(getApplicationContext()) {
                    @Override
                    public void fireAction() {
                        Handler handler = new Handler(getLooper());
                        handler.post(() -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl[0]));
                            File outputDir = getApplicationContext().getCacheDir();
                            try {
                                File outputFile = File.createTempFile(logRecordId.toString(), ".mp4", outputDir);
                                if (outputFile.length() == 0) {
                                    saveDataFromUrl(videoUrl[0], outputFile);
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

            case "ispChanged":
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_isp);
                description = logRecordData.get("isp") + ":" + logRecordData.get("ip");
                break;

            case "cameraReboot":
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_webcam);
                description = logRecordData.get("cameraName") + getResources().getString(R.string.notif_text_camera_is_rebooting);
                break;

            case "serverStateChanged":
                String action = (String) logRecordData.get("action");

                switch (action) {
                    case "started":
                        Long serverPid = (Long) logRecordData.get("pid");
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_server_started);
                        description = getResources().getString(R.string.notif_text_server_started) + serverPid;
                        break;

                    case "stopped":
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_server_stopped);
                        description = getResources().getString(R.string.notif_text_server_stopped);
                        break;

                    default:
                        Log.e(TAG, "Something wrong is happening with the server, please check urgently!");
                        break;
                }
                break;

            case "simpleNotification":
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_warning);
                description = logRecordData.get("simpleWatcherNotificationText").toString();
                break;

            default:
                Log.e(TAG, "Failed to process message with data: " + logRecordData);

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
