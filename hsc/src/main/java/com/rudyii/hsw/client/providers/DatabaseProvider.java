package com.rudyii.hsw.client.providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.rudyii.hsw.client.helpers.DBInitializer;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;

/**
 * Created by jack on 18.03.17.
 */

public class DatabaseProvider {

    private static SQLiteDatabase readableDB;
    private static SQLiteDatabase writableDB;

    private static SQLiteDatabase getReadableDatabase() {
        if (readableDB == null) {
            readableDB = new DBInitializer(getAppContext(), "settings.db", null, 1).getReadableDatabase();
        }

        return readableDB;
    }

    private static SQLiteDatabase getWritableDatabase() {
        if (writableDB == null) {
            writableDB = new DBInitializer(getAppContext(), "settings.db", null, 1).getWritableDatabase();
        }

        return writableDB;
    }

    public static void saveStringValueToSettings(String id, String value) {
        ContentValues values = new ContentValues();

        values.put("_ID", id);
        values.put("VALUE", value);

        getWritableDatabase().insertWithOnConflict("SETTINGS", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static String getStringValueFromSettings(String id) {
        String result = "";

        Cursor cursor = getReadableDatabase().rawQuery("SELECT VALUE FROM SETTINGS WHERE _ID = ?", new String[]{id});
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

        getWritableDatabase().insertWithOnConflict("SETTINGS", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static Long getLongValueFromSettings(String id) {
        Long result = 0L;

        Cursor cursor = getReadableDatabase().rawQuery("SELECT VALUE FROM SETTINGS WHERE _ID = ?", new String[]{id});
        if (cursor.moveToFirst()) {
            result = cursor.getLong(0);
        }
        cursor.close();

        return result;
    }

    public static void deleteIdFromSettings(String id) {
        getWritableDatabase().delete("SETTINGS", "_ID = ?", new String[]{id});
    }
}
