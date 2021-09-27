package com.rudyii.hsw.client.activities;

import static com.rudyii.hsw.client.providers.DatabaseProvider.getAllServers;

import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.ServerListAdapter;
import com.rudyii.hsw.client.objects.internal.ServerData;

import java.util.ArrayList;
import java.util.Map;

public class SelectServerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.please_select_active_server));
        setContentView(R.layout.activity_select_item);
        ListView serversButtons = findViewById(R.id.itemButton);

        Map<String, ServerData> allServers = getAllServers();

        if (allServers.isEmpty()) {
            finish();
        } else {
            ServerListAdapter serverListAdapter = new ServerListAdapter(new ArrayList<>(allServers.values()), this);
            serversButtons.setAdapter(serverListAdapter);
        }
    }
}
