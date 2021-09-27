package com.rudyii.hsw.client.activities;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_CAMERA;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_ROOT;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getActiveServerRootReference;

import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.settings.CameraSettings;
import com.rudyii.hsw.client.R;
import com.rudyii.hsw.client.helpers.CameraListAdapter;
import com.rudyii.hsw.client.objects.internal.CameraSettingsInternal;

import java.util.ArrayList;
import java.util.List;

public class SelectCameraActivity extends AppCompatActivity {
    private List<CameraSettingsInternal> cameraSettingsInternals = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.please_select_camera));
        setContentView(R.layout.activity_select_item);

        DatabaseReference cameraSettings = getActiveServerRootReference().child(SETTINGS_ROOT).child(SETTINGS_CAMERA);
        ValueEventListener cameraSettingsValueEventListener = buildCameraSettingsValueEventListener();
        cameraSettings.addListenerForSingleValueEvent(cameraSettingsValueEventListener);
    }

    private ValueEventListener buildCameraSettingsValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    snapshot.getChildren().forEach(dataSnapshot -> {
                        CameraSettings cameraSettings = dataSnapshot.getValue(CameraSettings.class);
                        cameraSettingsInternals.add(new CameraSettingsInternal(cameraSettings, dataSnapshot.getKey()));
                    });
                    CameraListAdapter cameraListAdapter = new CameraListAdapter(cameraSettingsInternals, getThis());
                    ListView camerasButton = findViewById(R.id.itemButton);
                    camerasButton.setAdapter(cameraListAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }

    private SelectCameraActivity getThis() {
        return this;
    }
}
