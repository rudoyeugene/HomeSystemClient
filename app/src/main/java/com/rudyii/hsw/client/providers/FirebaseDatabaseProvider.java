package com.rudyii.hsw.client.providers;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.rudyii.hsw.client.helpers.Utils.getServerKey;

/**
 * Created by j-a-c on 08.12.2017.
 */

public class FirebaseDatabaseProvider {
    private static FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    public static DatabaseReference getRootReference() {
        String serverKey = getServerKey();

        return firebaseDatabase.getReference(serverKey);
    }
}
