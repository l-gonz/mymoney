package com.example.laura.mymoney;

import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.laura.mymoney.data.PurseContract.PurseEntry;
import com.example.laura.mymoney.data.TransactionContract.TransactionEntry;

import java.util.Date;
import java.util.Locale;


/**
 * Created by Laura on 2/9/17.
 */

public class TransactionActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    int mTransType;
    int mPurseId;
    Uri mPurseUri;

    String mPurseName;
    int mPurseTotal;
    int mPurseType;
    int mPurseCurrency;

    private static final int defaultID = -1;
    private static final int LOADER_ID = 0;

    Calendar mCalendar;
    private static final String DATE_FORMAT = "d MMM yyyy";
    private static final String TIME_FORMAT = "HH:mm";
    SimpleDateFormat mDateFormat;
    SimpleDateFormat mTimeFormat;

    EditText mDateEditText;
    EditText mTimeEditText;
    EditText mConceptEditText;
    EditText mPlaceEditText;
    EditText mAmountEditText;
    TextView mCurrencyTextView;
    Button mPhotoButton;

    // TODO: Implement quick add ¿¿nougat icons??
    // TODO: Implement bank add from mail

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        // Get info from the intent that started the editor
        Intent intent = getIntent();
        mPurseId = intent.getIntExtra("PURSE_ID", defaultID);
        mTransType = intent.getIntExtra("TYPE", defaultID);
        mPurseUri = ContentUris.withAppendedId(PurseEntry.CONTENT_URI, mPurseId);

        // Get views from layout
        mDateEditText = (EditText) findViewById(R.id.transaction_date);
        mTimeEditText = (EditText) findViewById(R.id.transaction_time);
        mConceptEditText = (EditText) findViewById(R.id.transaction_concept);
        mPlaceEditText = (EditText) findViewById(R.id.transaction_place);
        mAmountEditText = (EditText) findViewById(R.id.transaction_amount);
        mCurrencyTextView = (TextView) findViewById(R.id.transaction_currency);
        mPhotoButton = (Button) findViewById(R.id.add_receipt_button);

        // Set current date and time in pickers
        mCalendar = Calendar.getInstance();
        mCalendar.setTime(new Date());

        mDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.forLanguageTag("ES"));
        mTimeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.forLanguageTag("ES"));
        mDateEditText.setText(mDateFormat.format(mCalendar.getTime()));
        mTimeEditText.setText(mTimeFormat.format(mCalendar.getTime()));

        setDatePicker();
        setTimePicker();

        // Launch camera from button
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: You know
                // https://developer.android.com/training/camera/photobasics.html
            }
        });

        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_transaction, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Check that the user has written at least the name
                String concept = mConceptEditText.getText().toString().trim();
                if (TextUtils.isEmpty(concept)) {
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    // Insert into the database
                    saveTransaction();
                }
                // Finish editing
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTransaction() {

        // Get the values from the data entered
        String concept = mConceptEditText.getText().toString().trim();
        String place = mPlaceEditText.getText().toString().trim();
        long date = mCalendar.getTimeInMillis() / 1000;
        int amount;
        try {
            amount = (int) (Float.parseFloat(mAmountEditText.getText().toString()) * 100);
        } catch (NumberFormatException e) {
            amount = 0;
        }
        // Calcular nuevo total
        if (mTransType == TransactionEntry.TYPE_NEGATIVE) {
            mPurseTotal -= amount;
        } else {
            mPurseTotal += amount;
        }
        // TODO: Get file path
        String receipt = "";

        // Create a new map of values, where column names are the keys
        ContentValues transValues = new ContentValues();
        transValues.put(TransactionEntry.COLUMN_CONCEPT, concept);
        transValues.put(TransactionEntry.COLUMN_DATE, date);
        transValues.put(TransactionEntry.COLUMN_AMOUNT, amount);
        transValues.put(TransactionEntry.COLUMN_TYPE, mTransType);
        transValues.put(TransactionEntry.COLUMN_PLACE, place);
        transValues.put(TransactionEntry.COLUMN_PURSE, mPurseId);
        transValues.put(TransactionEntry.COLUMN_TOTAL, mPurseTotal);
        transValues.put(TransactionEntry.COLUMN_CURRENCY, mPurseCurrency);
        transValues.put(TransactionEntry.COLUMN_RECEIPT, receipt);

        ContentValues purseValues = new ContentValues();
        purseValues.put(PurseEntry.COLUMN_NAME, mPurseName);
        purseValues.put(PurseEntry.COLUMN_TOTAL, mPurseTotal);
        purseValues.put(PurseEntry.COLUMN_TYPE, mPurseType);
        purseValues.put(PurseEntry.COLUMN_CURRENCY, mPurseCurrency);

        // Insert the new row, returning the primary key value of the new row
        Uri newUri = getContentResolver().insert(TransactionEntry.CONTENT_URI, transValues);
        // Update purse;
        int rowsUpdated = getContentResolver().update(mPurseUri, purseValues, null, null);
        Log.i("saveTransaction", "rowsUpdated: " + rowsUpdated);

        //Show toast message
        if (newUri != null && rowsUpdated != 0) {
            Toast.makeText(this, R.string.editor_saved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.editor_not_saved, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, mPurseUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor.moveToFirst()) {
            // Get data from the cursor
            mPurseName = cursor.getString(cursor.getColumnIndex(PurseEntry.COLUMN_NAME));
            mPurseTotal = cursor.getInt(cursor.getColumnIndex(PurseEntry.COLUMN_TOTAL));
            mPurseType = cursor.getInt(cursor.getColumnIndex(PurseEntry.COLUMN_TYPE));
            mPurseCurrency = cursor.getInt(cursor.getColumnIndex(PurseEntry.COLUMN_CURRENCY));

            mCurrencyTextView.setText(PurseEntry.selectCurrencySymbol(mPurseCurrency));
        }

        cursor.close();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    void setDatePicker () {
        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                mDateEditText.setText(mDateFormat.format(mCalendar.getTime()));
            }
        };

        mDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(TransactionActivity.this, dateSetListener, mCalendar.get(Calendar.YEAR),
                        mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    void setTimePicker () {
        final TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                mCalendar.set(Calendar.HOUR_OF_DAY, hour);
                mCalendar.set(Calendar.MINUTE, minute);

                mDateEditText.setText(mTimeFormat.format((mCalendar.getTime())));
            }
        };

        mTimeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(TransactionActivity.this, timeSetListener,
                        mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), true).show();
            }
        });
    }

}
