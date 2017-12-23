package com.rudyii.hsw.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rudyii.hsw.client.services.FirebaseService;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;

/**
 * Created by j-a-c on 11.12.2017.
 */

public class Autorun extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent bootIntent) {

        Log.i(TAG, "Autorun executed");

        Intent intent = new Intent(context, FirebaseService.class);
        context.startService(intent);
    }
}
