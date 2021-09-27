package com.rudyii.hsw.client.providers;

import static com.rudyii.hsw.client.helpers.Utils.getActiveServer;
import static com.rudyii.hsw.client.helpers.Utils.stringIsEmptyOrNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rudyii.hsw.client.objects.internal.ServerData;

/**
 * Created by Jack on 08.12.2017.
 */

public class FirebaseDatabaseProvider {
    private static final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    public static DatabaseReference getActiveServerRootReference() {
        ServerData activeServer = getActiveServer();
        String serverKey = "new_client_initiated";

        if (activeServer != null) {
            serverKey = activeServer.getServerKey();
        }

        if (stringIsEmptyOrNull(serverKey)) {
            return firebaseDatabase.getReference("nullReference");
        } else {
            return firebaseDatabase.getReference(serverKey);
        }
    }

    public static DatabaseReference getCustomRootReference(String serverKey) {
        return firebaseDatabase.getReference(serverKey);
    }

    public static DatabaseReference getActiveServerRootReference(String ref) {
        return firebaseDatabase.getReference(ref);
    }
}
