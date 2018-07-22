package com.rudyii.hsw.client.services;

import com.google.firebase.iid.FirebaseInstanceIdService;

import static com.rudyii.hsw.client.helpers.Utils.registerUserDataOnServers;

/**
 * Created by Jack on 26.12.2017.
 */

public class FCMService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        registerUserDataOnServers();
    }
}
