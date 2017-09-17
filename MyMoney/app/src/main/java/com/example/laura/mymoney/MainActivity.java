package com.example.laura.mymoney;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.example.laura.mymoney.adapters.PurseCursorAdapter;
import com.example.laura.mymoney.data.PurseContract.PurseEntry;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    PurseCursorAdapter mPurseAdapter;

    TextView mTotalTextView;

    private static final int PURSE_LOADER_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTotalTextView = (TextView) findViewById(R.id.total_text_view);

        // Set the adapter on the {@link GridView}
        // so the list can be populated in the user interface
        final GridView purseGridView = (GridView) findViewById(R.id.grid_view);
        mPurseAdapter = new PurseCursorAdapter(this, null);
        purseGridView.setAdapter(mPurseAdapter);
        // Set empty view on the GridView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_purses);
        purseGridView.setEmptyView(emptyView);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_main);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        purseGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                intent.setData(ContentUris.withAppendedId(PurseEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });

        purseGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                SharedPreferences sharedPreferences = getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(getString(R.string.default_purse_preferences), (int) id);
                editor.commit();

                purseGridView.setAdapter(mPurseAdapter);
                return true;
            }
        });

        // Prepare the cursor loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(PURSE_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(this, PurseEntry.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mPurseAdapter.swapCursor(cursor);

        // Sumar el total de todos los monederos
        int currency;
        int total = 0;
        if (!cursor.moveToFirst()) {
            return;
        }

        do {
            currency = cursor.getInt(cursor.getColumnIndexOrThrow(PurseEntry.COLUMN_CURRENCY));
            if (currency == PurseEntry.EUR) {
                total += cursor.getInt(cursor.getColumnIndexOrThrow(PurseEntry.COLUMN_TOTAL));
            }
        } while (cursor.moveToNext());
        mTotalTextView.setText(PurseEntry.formatTotal(total) + PurseEntry.selectCurrencySymbol(PurseEntry.EUR));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mPurseAdapter.swapCursor(null);
    }
}
