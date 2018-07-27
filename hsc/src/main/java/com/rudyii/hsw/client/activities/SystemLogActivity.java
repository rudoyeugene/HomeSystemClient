package com.rudyii.hsw.client.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.LogItem;
import com.rudyii.hsw.client.helpers.LogListAdapter;
import com.rudyii.hsw.client.helpers.ToastDrawer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.buildDataForMainActivityFrom;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateDoubleDotsDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateSingleDotDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.saveImageFromCamera;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;
import static java.util.Collections.reverse;

/**
 * Created by Jack on 14.01.2018.
 */

public class SystemLogActivity extends AppCompatActivity {
    public static final String HSC_SYSTEM_LOG_ITEM_CLICKED = "com.rudyii.hsw.client.HSC_SYSTEM_LOG_ITEM_CLICKED";

    @SuppressWarnings("FieldCanBeLocal")
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    @SuppressWarnings("FieldCanBeLocal")
    private LinearLayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private final ArrayList<LogItem> systemLog = new ArrayList<>();
    private DatabaseReference logRef;
    private ValueEventListener logValueEventListener;
    private final SystemLogBroadcastReceiver systemLogBroadcastReceiver = new SystemLogBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "SystemLog Activity created");

        setContentView(R.layout.activity_system_log);
        setTitle(getResources().getString(R.string.label_system_log));

        logRef = getRootReference().child("/log");
        logValueEventListener = getValueEventListener();

        logRef.addValueEventListener(logValueEventListener);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(false);

        mRecyclerView = (RecyclerView) findViewById(R.id.logRecyclerView);
        mRecyclerView.setHasFixedSize(false);

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(true);
        mRecyclerView.setItemAnimator(itemAnimator);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new LogListAdapter(getApplicationContext(), systemLog);
        mAdapter.setHasStableIds(true);

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

                reverse(systemLog);

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
        Intent intent;
        LogItem logItem = null;
        Bitmap image = null;
        String description = null;

        switch (reason) {
            case "systemStateChanged":
                String armedMode = (String) logRecordData.get("armedMode");
                String armedState = (String) logRecordData.get("armedState");
                Boolean portsOpen = Boolean.valueOf(logRecordData.get("portsOpen").toString());

                HashMap<String, Object> statusesData = buildDataForMainActivityFrom(armedMode, armedState, portsOpen);

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
                        + ":" + statusesData.get("systemStateText")
                        + ", " + ((boolean) statusesData.get("portsState") ? getResources().getString(R.string.notif_text_ports_open_text) : getResources().getString(R.string.notif_text_ports_closed_text));
                break;

            case "motionDetected":
                String imageString = (String) logRecordData.get("image");
                String serverName = (String) logRecordData.get("serverName");
                String cameraName = (String) logRecordData.get("cameraName");

                byte[] decodedImageString = Base64.decode(imageString, Base64.DEFAULT);

                image = BitmapFactory.decodeByteArray(decodedImageString, 0, decodedImageString.length);
                description = logRecordData.get("cameraName") + ":" + logRecordData.get("motionArea") + "%";

                String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HomeSystemMotions/" + serverName + "/" + cameraName + "/";
                String imageName = getCurrentTimeAndDateSingleDotDelimFrom(logRecordId);
                String fileLocation = directory + imageName + ".jpg";
                File imageFile = new File(fileLocation);

                if (!imageFile.exists()) {
                    saveImageFromCamera(image, serverName, cameraName, getCurrentTimeAndDateSingleDotDelimFrom(logRecordId));
                }

                Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".com.rudyii.hsc", imageFile);

                intent = new Intent(Intent.ACTION_VIEW, photoURI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                final Intent finalIntentForMotion = intent;
                logItem = new LogItem(getApplicationContext()) {
                    @Override
                    public void fireAction() {
                        startActivity(finalIntentForMotion);
                    }
                };

                logItem.fill(image, title, description, logRecordId);
                break;

            case "videoRecorded":
                image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_video);
                description = logRecordData.get("fileName").toString();
                String url = (String) logRecordData.get("url");
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                final Intent finalIntentForVideoRecorded = intent;
                logItem = new LogItem(getApplicationContext()) {
                    @Override
                    public void fireAction() {
                        startActivity(finalIntentForVideoRecorded);
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

            case "serverStartupOrShutdown":
                String action = (String) logRecordData.get("action");

                switch (action) {
                    case "starting":
                        Long serverPid = (Long) logRecordData.get("pid");
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_server_started);
                        description = getResources().getString(R.string.notif_text_server_started) + serverPid;
                        break;

                    case "stopping":
                        image = BitmapFactory.decodeResource(getResources(), R.mipmap.image_server_stopped);
                        description = getResources().getString(R.string.notif_text_server_stopped);
                        break;

                    default:
                        Log.e(TAG, "Something wrong is happening with the server, please check urgently!");
                        break;
                }
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
