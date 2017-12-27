package com.rudyii.hsw.client.services;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.helpers.Utils.getSimplifiedPrimaryAccountName;
import static com.rudyii.hsw.client.providers.FirebaseDatabaseProvider.getRootReference;

/**
 * Created by j-a-c on 26.12.2017.
 */

public class FCMService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        String accountName = getSimplifiedPrimaryAccountName();

        if (!accountName.equals("")) {
            getRootReference().child("/connectedClients/" + accountName).setValue(refreshedToken);
        }
    }
}
