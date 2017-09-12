package com.example.laura.mymoney.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.laura.mymoney.data.PurseContract.PurseEntry;
import com.example.laura.mymoney.data.TransactionContract.TransactionEntry;

/**
 * Created by Laura on 11/6/17.
 */

public class MoneyDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "mymoney.db";

    public MoneyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        //SQL command for creating the table
        String SQL_CREATE_TABLE_PURSES = "CREATE TABLE " + PurseEntry.TABLE_NAME + " (" +
                PurseEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PurseEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                PurseEntry.COLUMN_TOTAL + " INTEGER NOT NULL, " +
                PurseEntry.COLUMN_TYPE + " INTEGER NOT NULL, " +
                PurseEntry.COLUMN_CURRENCY + " INTEGER NOT NULL DEFAULT 0" + ");";

        String SQL_CREATE_TABLE_TRANSACTIONS = "CREATE TABLE " + TransactionEntry.TABLE_NAME + " (" +
                TransactionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TransactionEntry.COLUMN_CONCEPT + " TEXT NOT NULL, " +
                TransactionEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_AMOUNT + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_TYPE + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_PLACE + " TEXT, " +
                TransactionEntry.COLUMN_PURSE + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_TOTAL + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_CURRENCY + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_RECEIPT + " TEXT" + ");";

        db.execSQL(SQL_CREATE_TABLE_PURSES);
        db.execSQL(SQL_CREATE_TABLE_TRANSACTIONS);
        Log.i("DBHelper", "Tables created");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
