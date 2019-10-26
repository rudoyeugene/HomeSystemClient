package com.rudyii.hsw.client.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rudyii.hsw.client.R;

import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;
import static java.util.Objects.requireNonNull;

/**
 * Created by Jack on 29.12.2017.
 */

public class ShortcutsActionProcessor extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortcuts);

        TextView shortcutsActivityDialogText = findViewById(R.id.shortcutsActivityDialogText);

        TextView textView = findViewById(android.R.id.title);
        if (textView != null) {
            textView.setGravity(Gravity.CENTER);
        }

        String action = getIntent().getAction();

        if (action == null) {
            Log.e(TAG, "Failed to process shortcut action");
            finish();
        }

        Map<String, String> stateRequest = new HashMap<>();

        switch (requireNonNull(action)) {
            case "com.rudyii.hsw.client.ARM":
                stateRequest.put("armedMode", "MANUAL");
                stateRequest.put("armedState", "ARMED");
                shortcutsActivityDialogText.setText(getResources().getString(R.string.shortcut_arm_activity_text));
                break;

            case "com.rudyii.hsw.client.DISARM":
                stateRequest.put("armedMode", "MANUAL");
                stateRequest.put("armedState", "DISARMED");
                shortcutsActivityDialogText.setText(getResources().getString(R.string.shortcut_disarm_activity_text));
                break;

            case "com.rudyii.hsw.client.AUTO":
                stateRequest.put("armedMode", "AUTOMATIC");
                stateRequest.put("armedState", "AUTO");
                shortcutsActivityDialogText.setText(getResources().getString(R.string.shortcut_auto_activity_text));
                break;
        }

        getRootReference().child("requests/state").setValue(stateRequest);
        setFinishOnTouchOutside(true);
    }
}
