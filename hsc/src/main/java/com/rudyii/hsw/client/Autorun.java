package com.rudyii.hsw.client;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rudyii.hsw.client.services.FCMMessagingService;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;

/**
 * Created by Jack on 11.12.2017.
 */

public class Autorun extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent bootIntent) {

        Log.i(TAG, "Autorun executed");

        Intent intent = new Intent(context, FCMMessagingService.class);
        context.startService(intent);
    }
}
