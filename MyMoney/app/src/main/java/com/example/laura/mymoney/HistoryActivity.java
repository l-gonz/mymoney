package com.example.laura.mymoney;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.laura.mymoney.adapters.TransactionCursorAdapter;
import com.example.laura.mymoney.data.PurseContract.PurseEntry;
import com.example.laura.mymoney.data.TransactionContract.TransactionEntry;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.util.Date;

/**
 * Created by Laura on 31/8/17.
 */

public class HistoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    TextView mTotalTextView;
    TextView mDateTextView;

    TransactionCursorAdapter mAdapter;

    int mMonth;
    int mYear;
    int mTypePurse;

    /** Particular uri for the edit mode on EditorActivity */
    private Uri mPurseUri;
    private int mPurseId;

    private static final int TRANSACTION_LOADER_ID = 0;
    private static final int PURSE_LOADER_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("HistoryActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Get info from the intent that started the history
        Intent intent = getIntent();
        mPurseUri = intent.getData();
        mPurseId = (int) ContentUris.parseId(mPurseUri);
        Log.i("HistoryActivity", "id: " + mPurseId);

        // Find all relevant views that we will need to read user input from
        mTotalTextView = (TextView) findViewById(R.id.history_overview_textview);
        ListView listView = (ListView) findViewById(R.id.history_list_view);
        mDateTextView = (TextView) findViewById(R.id.date_text_view);

        // Set initial values for date
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        mMonth = calendar.get(Calendar.MONTH);
        mYear = calendar.get(Calendar.YEAR);
        mDateTextView.setText(TransactionEntry.formatDateLong(calendar));

        // Set up cursor adapter
        mAdapter = new TransactionCursorAdapter(this, null);
        listView.setAdapter(mAdapter);
        // Set empty view on the GridView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_transactions);
        listView.setEmptyView(emptyView);

        // Floating action buttons
        FloatingActionButton incomeFab = (FloatingActionButton) findViewById(R.id.fab_income);
        incomeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HistoryActivity.this, TransactionActivity.class);
                intent.putExtra("PURSE_ID", mPurseId);
                intent.putExtra("TYPE", TransactionEntry.TYPE_POSITIVE);
                startActivity(intent);
            }
        });

        FloatingActionButton expenseFab = (FloatingActionButton) findViewById(R.id.fab_expense);
        expenseFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HistoryActivity.this, TransactionActivity.class);
                intent.putExtra("PURSE_ID", mPurseId);
                intent.putExtra("TYPE", TransactionEntry.TYPE_NEGATIVE);
                startActivity(intent);
            }
        });

        // TODO: Set up item click
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Start overview transaction activity
            }
        });

        // Avanza año
        ImageButton nextButton = (ImageButton) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMonth < calendar.getMaximum(Calendar.MONTH)) {
                    mMonth ++;
                } else {
                    mYear ++;
                    mMonth = calendar.getMinimum(Calendar.MONTH);
                }

                calendar.set(mYear, mMonth, calendar.getMinimum(Calendar.DAY_OF_MONTH));
                mDateTextView.setText(TransactionEntry.formatDateLong(calendar));
                getLoaderManager().restartLoader(TRANSACTION_LOADER_ID, null, HistoryActivity.this);
            }
        });

        // Retrocede año
        ImageButton beforeButton = (ImageButton) findViewById(R.id.before_button);
        beforeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMonth > calendar.getMinimum(Calendar.MONTH)) {
                    mMonth --;
                } else {
                    mYear --;
                    mMonth = calendar.getMaximum(Calendar.MONTH);
                }

                calendar.set(mYear, mMonth, calendar.getMinimum(Calendar.DAY_OF_MONTH));
                mDateTextView.setText(TransactionEntry.formatDateLong(calendar));
                getLoaderManager().restartLoader(TRANSACTION_LOADER_ID, null, HistoryActivity.this);
            }
        });

        // Prepare the cursor loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(TRANSACTION_LOADER_ID, null, this);
        getLoaderManager().initLoader(PURSE_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Edit" menu option
            case R.id.action_edit:
                Intent intent = new Intent(HistoryActivity.this, EditorActivity.class);
                intent.setData(mPurseUri);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case TRANSACTION_LOADER_ID:
                // Get calendar objects that point to the first and last day of the selected month
                GregorianCalendar startMonth = new GregorianCalendar(mYear, mMonth, 1);
                int lastDay = startMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
                GregorianCalendar endMonth = new GregorianCalendar(mYear, mMonth, lastDay);

                // Turn calendar into Unix Timestamp
                long startEpoch = startMonth.getTimeInMillis() / 1000;
                long endEpoch = endMonth.getTimeInMillis() / 1000;

                // Select entries between the two dates above
                String selection = TransactionEntry.COLUMN_DATE + " BETWEEN ? AND ? AND " +
                        TransactionEntry.COLUMN_PURSE + "=?";
                String[] selectionArgs = new String[]{String.valueOf(startEpoch), String.valueOf(endEpoch),
                        String.valueOf(mPurseId)};
                String sortOrder = TransactionEntry.COLUMN_DATE + " DESC";

                return new CursorLoader(this, TransactionEntry.CONTENT_URI, null, selection,
                        selectionArgs, sortOrder);
            case PURSE_LOADER_ID:
                return new CursorLoader(this, mPurseUri, null, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (loader.getId() == TRANSACTION_LOADER_ID) {
            // Swap the new cursor in.  (The framework will take care of closing the
            // old cursor once we return.)
            mAdapter.swapCursor(cursor);
        } else if (loader.getId() == PURSE_LOADER_ID) {
            // TODO: Hacer algo para que se actualice al editarlo y volver
            Cursor c = getContentResolver().query(mPurseUri, null, null, null, null);
            if (c.moveToFirst()) {
                // Get data from the cursor
                String purseTitle = c.getString(c.getColumnIndex(PurseEntry.COLUMN_NAME));
                int total = c.getInt(c.getColumnIndex(PurseEntry.COLUMN_TOTAL));
                mTypePurse = c.getInt(c.getColumnIndex(PurseEntry.COLUMN_TYPE));
                int currency = c.getInt(c.getColumnIndex(PurseEntry.COLUMN_CURRENCY));

                // Set data on the UI
                setTitle(purseTitle);
                mTotalTextView.setText(PurseEntry.formatTotal(total) + PurseEntry.selectCurrencySymbol(currency));
            }
            c.close();
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == TRANSACTION_LOADER_ID) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
            mAdapter.swapCursor(null);
            Log.i("HistoryActivity", "onLoaderReset");
        } else if (loader.getId() == PURSE_LOADER_ID) {
            // Do nothing
        }
    }

}
