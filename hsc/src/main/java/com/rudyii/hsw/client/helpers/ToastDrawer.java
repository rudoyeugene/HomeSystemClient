package com.rudyii.hsw.client.helpers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.getLooper;

import android.os.Handler;
import android.widget.Toast;

/**
 * Created by Jack on 15.10.2017.
 */

public class ToastDrawer {
    public void showToast(final String message) {
        Handler h = new Handler(getLooper());
        h.post(() -> Toast.makeText(getAppContext(), message, Toast.LENGTH_SHORT).show());
    }

    public void showLongToast(final String message) {
        Handler h = new Handler(getLooper());
        h.post(() -> Toast.makeText(getAppContext(), message, Toast.LENGTH_LONG).show());
    }
}
