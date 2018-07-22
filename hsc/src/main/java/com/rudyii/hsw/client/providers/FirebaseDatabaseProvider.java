package com.rudyii.hsw.client.providers;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.rudyii.hsw.client.helpers.Utils.getActiveServerKey;
import static com.rudyii.hsw.client.helpers.Utils.stringIsEmptyOrNull;

/**
 * Created by Jack on 08.12.2017.
 */

public class FirebaseDatabaseProvider {
    private static final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    public static DatabaseReference getRootReference() {
        String serverKey = getActiveServerKey();

        if (stringIsEmptyOrNull(serverKey)){
            return firebaseDatabase.getReference("nullReference");
        } else {
            return firebaseDatabase.getReference(serverKey);
        }
    }

    public static DatabaseReference getCustomReference(String ref) {
        return firebaseDatabase.getReference(ref);
    }
}
