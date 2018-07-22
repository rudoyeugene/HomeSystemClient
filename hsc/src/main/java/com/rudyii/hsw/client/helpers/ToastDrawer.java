package com.rudyii.hsw.client.helpers;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;

/**
 * Created by Jack on 15.10.2017.
 */

public class ToastDrawer {
    public void showToast(final String message) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> Toast.makeText(getAppContext(), message, Toast.LENGTH_SHORT).show());
    }

    public void showLongToast(final String message) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> Toast.makeText(getAppContext(), message, Toast.LENGTH_LONG).show());
    }
}
