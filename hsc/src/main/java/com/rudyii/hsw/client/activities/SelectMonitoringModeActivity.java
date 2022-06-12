package com.rudyii.hsw.client.activities;

import static java.util.Arrays.asList;

import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.rudyii.hs.common.type.MonitoringModeType;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.MonitoringModeListAdapter;

import java.util.ArrayList;

public class SelectMonitoringModeActivity extends AppCompatActivity {
    private ListView buttons;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.please_select_desired_system_mode_and_state));
        setContentView(R.layout.activity_select_item);
        buttons = findViewById(R.id.itemButton);
        MonitoringModeListAdapter adapter = new MonitoringModeListAdapter(new ArrayList<>(asList(MonitoringModeType.values())), this);
        buttons.setAdapter(adapter);
    }
}
