package com.rudyii.hsw.client.activities;

import static com.rudyii.hsw.client.providers.DatabaseProvider.getAllServers;
import static java.util.Arrays.asList;

import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.rudyii.hs.common.type.SystemModeType;
import com.rudyii.hs.common.type.SystemStateType;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.SystemModeAndStateListAdapter;
import com.rudyii.hsw.client.objects.internal.ServerData;
import com.rudyii.hsw.client.objects.internal.SystemModeAndState;

import java.util.ArrayList;
import java.util.Map;

public class SelectModeAndStateActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.please_select_desired_system_mode_and_state));
        setContentView(R.layout.activity_select_item);
        ListView buttons = findViewById(R.id.itemButton);

        Map<String, ServerData> allServers = getAllServers();

        if (allServers.isEmpty()) {
            finish();
        } else {
            SystemModeAndStateListAdapter adapter = new SystemModeAndStateListAdapter(new ArrayList<>(asList(
                    SystemModeAndState.builder()
                            .systemMode(SystemModeType.AUTOMATIC)
                            .systemState(SystemStateType.RESOLVING)
                            .build(),
                    SystemModeAndState.builder()
                            .systemMode(SystemModeType.MANUAL)
                            .systemState(SystemStateType.ARMED)
                            .build(),
                    SystemModeAndState.builder()
                            .systemMode(SystemModeType.MANUAL)
                            .systemState(SystemStateType.DISARMED)
                            .build()
            )), this);
            buttons.setAdapter(adapter);
        }
    }
}
