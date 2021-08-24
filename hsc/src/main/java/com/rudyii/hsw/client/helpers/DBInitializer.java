package com.rudyii.hsw.client.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jack on 30.04.17.
 */

public class DBInitializer extends SQLiteOpenHelper {
    public static final String SETTINGS_TABLE = "SETTINGS";
    public static final String SERVERS_TABLE = "SERVERS";

    public DBInitializer(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SETTINGS_TABLE + " (_ID TEXT UNIQUE, VALUE TEXT)");
        db.execSQL("CREATE TABLE " + SERVERS_TABLE + " (_ID TEXT UNIQUE, VALUE TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
