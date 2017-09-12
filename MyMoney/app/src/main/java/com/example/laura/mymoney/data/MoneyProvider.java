package com.example.laura.mymoney.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.laura.mymoney.data.TransactionContract.TransactionEntry;
import com.example.laura.mymoney.data.PurseContract.PurseEntry;

/**
 * {@link ContentProvider} for MyMoney app.
 */
public class MoneyProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = MoneyProvider.class.getSimpleName();

    MoneyDbHelper mDbHelper;

    // URI matcher code for the content URI for the purses table
    private static final int PURSES = 100;
    // URI matcher code for the content URI for a single purse in the purses table
    private static final int PURSE_ID = 101;
    // URI matcher code for the content URI for the history table
    private static final int TRANSACTIONS = 200;
    // URI matcher code for the content URI for a single history entry
    private static final int TRANSACTION_ID = 201;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        sUriMatcher.addURI(PurseContract.CONTENT_AUTHORITY, PurseContract.PATH_PURSES, PURSES);
        sUriMatcher.addURI(PurseContract.CONTENT_AUTHORITY, PurseContract.PATH_PURSES + "/#", PURSE_ID);
        sUriMatcher.addURI(TransactionContract.CONTENT_AUTHORITY,
                TransactionContract.PATH_TRANSACTIONS, TRANSACTIONS);
        sUriMatcher.addURI(TransactionContract.CONTENT_AUTHORITY,
                TransactionContract.PATH_TRANSACTIONS + "/#", TRANSACTION_ID);
    }


     /**
      * Initialize the provider and the database helper object.
      */
    @Override
    public boolean onCreate() {
        // Initialize a MoneyDbHelper object to gain access to the purses database.
        mDbHelper = new MoneyDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PURSES:
                // For the PETS code, query the pets table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                cursor = db.query(PurseEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PURSE_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = PurseEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = db.query(PurseEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case TRANSACTIONS:
                cursor = db.query(TransactionEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case TRANSACTION_ID:
                selection = TransactionEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                cursor = db.query(PurseEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PURSES:
                return insertPurse(uri, contentValues);
            case TRANSACTIONS:
                return insertEntry(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a pet into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertPurse(Uri uri, ContentValues values) {

        // Data validation
        String name = values.getAsString(PurseEntry.COLUMN_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Purse requires a name");
        }

        Integer total = values.getAsInteger(PurseEntry.COLUMN_TOTAL);
        if (total == null) {
            throw new IllegalArgumentException("Purse requires a total");
        }

        int type = values.getAsInteger(PurseEntry.COLUMN_TYPE);
        if (!PurseEntry.isTypeValid(type)) {
            throw new IllegalArgumentException("Purse requires a valid type");
        }

        int currency = values.getAsInteger(PurseEntry.COLUMN_CURRENCY);
        if (!PurseEntry.isCurrencyValid(currency)) {
            throw new IllegalArgumentException("Purse requires a valid currency");
        }

        // Insert a new pet into the pets database table with the given ContentValues
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long id = db.insert(PurseEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    private Uri insertEntry(Uri uri, ContentValues values) {

        // Data validation
        String concept = values.getAsString(TransactionEntry.COLUMN_CONCEPT);
        if (concept == null) {
            throw new IllegalArgumentException("Transaction requires a concept");
        }

        Integer date = values.getAsInteger(TransactionEntry.COLUMN_DATE);
        if (date == null) {
            throw new IllegalArgumentException("Transaction requires a date");
        }

        Integer quantity = values.getAsInteger(TransactionEntry.COLUMN_AMOUNT);
        if (quantity == null) {
            throw new IllegalArgumentException("Transaction requires a quantity");
        }

        int type = values.getAsInteger(TransactionEntry.COLUMN_TYPE);
        if (!TransactionEntry.isTypeValid(type)) {
            throw new IllegalArgumentException("Transaction requires a valid type");
        }

        Integer purse = values.getAsInteger(TransactionEntry.COLUMN_PURSE);
        if (purse == null) {
            throw new IllegalArgumentException("Transaction requires a purse");
        }

        Integer total = values.getAsInteger(TransactionEntry.COLUMN_TOTAL);
        if (total == null) {
            throw new IllegalArgumentException("Transaction requires a total");
        }

        // Insert a new transaction into the database table with the given ContentValues
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long id = db.insert(TransactionEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PURSES:
                return updatePurse(uri, contentValues, selection, selectionArgs);
            case PURSE_ID:
                // Delete a single row given by the ID in the URI
                selection = PurseEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updatePurse(uri, contentValues, selection, selectionArgs);
            case TRANSACTIONS:
                return updateTransaction(uri, contentValues, selection, selectionArgs);
            case TRANSACTION_ID:
                // Delete a single row given by the ID in the URI
                selection = TransactionEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updatePurse(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update purses in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updatePurse(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Data validation
        String name = values.getAsString(PurseEntry.COLUMN_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Purse requires a name");
        }

        Integer total = values.getAsInteger(PurseEntry.COLUMN_TOTAL);
        if (total == null) {
            throw new IllegalArgumentException("Purse requires a total");
        }

        int type = values.getAsInteger(PurseEntry.COLUMN_TYPE);
        Log.i("update", "Type " + type);
        if (!PurseEntry.isTypeValid(type)) {
            throw new IllegalArgumentException("Purse requires a valid type");
        }

        int currency = values.getAsInteger(PurseEntry.COLUMN_CURRENCY);
        if (!PurseEntry.isCurrencyValid(currency)) {
            throw new IllegalArgumentException("Purse requires a valid currency");
        }

        // Load database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = db.update(PurseEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
         return rowsUpdated;
    }

    private int updateTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Data validation
        String concept = values.getAsString(TransactionEntry.COLUMN_CONCEPT);
        if (concept == null) {
            throw new IllegalArgumentException("Transaction requires a concept");
        }

        Integer date = values.getAsInteger(TransactionEntry.COLUMN_DATE);
        if (date == null) {
            throw new IllegalArgumentException("Transaction requires a date");
        }

        Integer quantity = values.getAsInteger(TransactionEntry.COLUMN_AMOUNT);
        if (quantity == null) {
            throw new IllegalArgumentException("Transaction requires a quantity");
        }

        int type = values.getAsInteger(TransactionEntry.COLUMN_TYPE);
        if (!TransactionEntry.isTypeValid(type)) {
            throw new IllegalArgumentException("Transaction requires a valid type");
        }

        Integer purse = values.getAsInteger(TransactionEntry.COLUMN_PURSE);
        if (purse == null) {
            throw new IllegalArgumentException("Transaction requires a purse");
        }

        Integer total = values.getAsInteger(TransactionEntry.COLUMN_TOTAL);
        if (total == null) {
            throw new IllegalArgumentException("Transaction requires a total");
        }

        // Load database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = db.update(TransactionEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
         return rowsUpdated;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PURSES:
                // Delete all rows that match the selection and selection args
                rowsDeleted = db.delete(PurseEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PURSE_ID:
                // Delete a single row given by the ID in the URI
                selection = PurseEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = db.delete(PurseEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TRANSACTIONS:
                rowsDeleted = db.delete(TransactionEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TRANSACTION_ID:
                selection = TransactionEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = db.delete(TransactionEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PURSES:
                return PurseEntry.CONTENT_LIST_TYPE;
            case PURSE_ID:
                return PurseEntry.CONTENT_ITEM_TYPE;
            case TRANSACTIONS:
                return TransactionEntry.CONTENT_LIST_TYPE;
            case TRANSACTION_ID:
                return TransactionEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}