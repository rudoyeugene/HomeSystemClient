package com.rudyii.hsw.client.providers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.DBInitializer.SERVERS_TABLE;
import static com.rudyii.hsw.client.helpers.DBInitializer.SETTINGS_TABLE;
import static com.rudyii.hsw.client.helpers.Utils.ACTIVE_SERVER;
import static com.rudyii.hsw.client.helpers.Utils.buildFromRawJson;
import static com.rudyii.hsw.client.helpers.Utils.writeJson;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.rudyii.hsw.client.helpers.DBInitializer;
import com.rudyii.hsw.client.objects.internal.ServerData;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jack on 18.03.17.
 */

public class DatabaseProvider {
    private static SQLiteDatabase readableDB;
    private static SQLiteDatabase writableDB;

    private static SQLiteDatabase getReadableDatabase() {
        if (readableDB == null) {
            readableDB = new DBInitializer(getAppContext(), "app.db", null, 1).getReadableDatabase();
        }

        return readableDB;
    }

    private static SQLiteDatabase getWritableDatabase() {
        if (writableDB == null) {
            writableDB = new DBInitializer(getAppContext(), "app.db", null, 1).getWritableDatabase();
        }

        return writableDB;
    }

    public static void saveStringValueToSettings(String id, String value) {
        ContentValues values = new ContentValues();

        values.put("_ID", id);
        values.put("VALUE", value);

        getWritableDatabase().insertWithOnConflict(SETTINGS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void saveIntegerValueToSettings(String id, int value) {
        ContentValues values = new ContentValues();

        values.put("_ID", id);
        values.put("VALUE", value);

        getWritableDatabase().insertWithOnConflict(SETTINGS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void setOrUpdateActiveServer(ServerData serverData) {
        ContentValues values = new ContentValues();

        values.put("_ID", ACTIVE_SERVER);
        values.put("VALUE", writeJson(serverData));

        getWritableDatabase().insertWithOnConflict(SETTINGS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void addOrUpdateServer(ServerData serverData) {
        ContentValues values = new ContentValues();

        values.put("_ID", serverData.getServerKey());
        values.put("VALUE", writeJson(serverData));

        getWritableDatabase().insertWithOnConflict(SERVERS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);

    }

    public static ServerData getServer(String serverKey) {
        String result = null;

        Cursor cursor = getReadableDatabase().rawQuery("SELECT VALUE FROM " + SERVERS_TABLE + " WHERE _ID = ?", new String[]{serverKey});
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();

        if (result == null) {
            return ServerData.builder().build();
        } else {
            return buildFromRawJson(result, ServerData.class);
        }
    }

    public static Map<String, ServerData> getAllServers() {
        Map<String, ServerData> result = new HashMap<>();

        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + SERVERS_TABLE, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String serverKey = cursor.getString(0);
                String serverDataJson = cursor.getString(1);
                result.put(serverKey, buildFromRawJson(serverDataJson, ServerData.class));
                cursor.moveToNext();
            }
        }
        cursor.close();

        return result;
    }

    public static void removeServer(String serverKey) {
        getReadableDatabase().delete(SERVERS_TABLE, "_ID = ?", new String[]{serverKey});

    }

    public static String getStringValueFromSettings(String id) {
        String result = "";

        Cursor cursor = getReadableDatabase().rawQuery("SELECT VALUE FROM " + SETTINGS_TABLE + " WHERE _ID = ?", new String[]{id});
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();

        if (result == null) {
            return "";
        } else {
            return result;
        }
    }

    public static void saveLongValueToSettings(String id, Long value) {
        ContentValues values = new ContentValues();

        values.put("_ID", id);
        values.put("VALUE", value);

        getWritableDatabase().insertWithOnConflict(SETTINGS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static Long getLongValueFromSettings(String id) {
        long result = 0L;

        Cursor cursor = getReadableDatabase().rawQuery("SELECT VALUE FROM " + SETTINGS_TABLE + " WHERE _ID = ?", new String[]{id});
        if (cursor.moveToFirst()) {
            result = cursor.getLong(0);
        }
        cursor.close();

        return result;
    }

    public static int getIntValueFromSettings(String id) {
        int result = 0;

        Cursor cursor = getReadableDatabase().rawQuery("SELECT VALUE FROM " + SETTINGS_TABLE + " WHERE _ID = ?", new String[]{id});
        if (cursor.moveToFirst()) {
            result = cursor.getInt(0);
        }
        cursor.close();

        return result;
    }

    public static void deleteIdFromSettings(String id) {
        getWritableDatabase().delete(SETTINGS_TABLE, "_ID = ?", new String[]{id});
    }
}
