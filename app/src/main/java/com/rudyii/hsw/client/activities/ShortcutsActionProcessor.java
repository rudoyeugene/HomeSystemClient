package com.rudyii.hsw.client.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;

/**
 * Created by j-a-c on 29.12.2017.
 */

public class ShortcutsActionProcessor extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();

        if (action == null){
            Log.e(TAG, "Failed to process shortcut action");
            finish();
        }

        Map<String, String> stateRequest = new HashMap<>();

        switch (action) {
            case "com.rudyii.hsw.client.ARM":
                stateRequest.put("armedMode", "MANUAL");
                stateRequest.put("armedState", "ARMED");

                break;

            case "com.rudyii.hsw.client.DISARM":
                stateRequest.put("armedMode", "MANUAL");
                stateRequest.put("armedState", "DISARMED");

                break;

            case "com.rudyii.hsw.client.AUTO":
                stateRequest.put("armedMode", "AUTOMATIC");
                stateRequest.put("armedState", "AUTO");

                break;
        }

        getRootReference().child("requests/state").setValue(stateRequest);

        finish();
    }
}
