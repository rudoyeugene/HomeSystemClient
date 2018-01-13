package com.rudyii.hsw.client.helpers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import static com.rudyii.hsw.client.HomeSystemClientApplication.TAG;
import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.getCurrentTimeAndDateSingleDotDelimFrom;
import static com.rudyii.hsw.client.helpers.Utils.saveImageFromCamera;
import static com.rudyii.hsw.client.listeners.MotionListener.HSC_MOTION_DETECTED;

/**
 * Created by j-a-c on 27.12.2017.
 */

public class FirebaseListenersFactory {

    public static ValueEventListener buildMotionRefValueEventListener(final String serverName) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Map<String, Object> motion = (Map<String, Object>) dataSnapshot.getValue();

                if (motion == null) {
                    Log.w(TAG, "Got obsolete motion link!");
                    return;
                }

                Long motionTimeStamp = (Long) motion.get("timeStamp");

                HashMap<String, Object> motionData = new HashMap<>();
                motionData.put("cameraName", motion.get("cameraName"));
                motionData.put("timeStamp", motion.get("timeStamp"));
                motionData.put("motionArea", motion.get("motionArea"));
                motionData.put("serverName", serverName);

                String imageString = (String) motion.get("image");
                byte[] decodedImageString = Base64.decode(imageString, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImageString, 0, decodedImageString.length);

                saveImageFromCamera(bitmap, motionData.get("serverName").toString(), motionData.get("cameraName").toString(), getCurrentTimeAndDateSingleDotDelimFrom(motionTimeStamp));

                while (bitmap.getByteCount() > 512000) {
                    int srcWidth = bitmap.getWidth();
                    int srcHeight = bitmap.getHeight();
                    int dstWidth = (int) (srcWidth * 0.9f);
                    int dstHeight = (int) (srcHeight * 0.9f);
                    bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true);
                }

                motionData.put("image", bitmap);

                Intent intent = new Intent();
                intent.setAction(HSC_MOTION_DETECTED);
                intent.putExtra("HSC_MOTION_DETECTED", motionData);
                getAppContext().sendBroadcast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch motion data");
            }
        };
    }
}
