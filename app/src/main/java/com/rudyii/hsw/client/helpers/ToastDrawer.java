package com.rudyii.hsw.client.helpers;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;

/**
 * Created by j-a-c on 15.10.2017.
 */

public class ToastDrawer {
    public void showToast(final String subject, final String message) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            public void run() {
                Toast.makeText(getAppContext(), subject + " : " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
