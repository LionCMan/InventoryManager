package com.example.android.inventorymanager.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.android.inventorymanager.Data.ProductContract.ProductEntry;

public class ProductDbHelper extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "inventory.db";
    public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + ProductEntry.TABLE_NAME;
    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + ProductEntry.TABLE_NAME + " ( " +
            ProductEntry._ID + "INTEGER PRIMARY KEY AUTOINCREMENT, " +
            ProductEntry.COLUMN_NAME + "TEXT NOT NULL, " +
            ProductEntry.COLUMN_PRICE + "REAL NOT NULL DEFAULT 0, " +
            ProductEntry.COLUMN_QUANTITY + "INTEGER NOT NULL DEFAULT 0, " +
            ProductEntry.COLUMN_IMAGE + "TEXT NOT NULL );";

    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.i("SQLENTRIES", SQL_CREATE_ENTRIES);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table or read from existent
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing :)
    }
}
