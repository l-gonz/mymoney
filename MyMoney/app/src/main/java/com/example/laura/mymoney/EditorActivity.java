package com.example.laura.mymoney;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.laura.mymoney.data.PurseContract.PurseEntry;

/**
 * Created by Laura on 26/8/17.
 */

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private EditText mNameEditText;
    private EditText mTotalEditText;
    private Spinner mTypeSpinner;
    private Spinner mCurrencySpinner;

    private int mType = PurseEntry.TYPE_CASH;
    private int mCurrency = PurseEntry.EUR;

    private static final int LOADER_ID = 0;

    private boolean mPurseHasChanged = false;

    /** Particular uri for the edit mode */
    private Uri mPurseUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_purse_name);
        mTotalEditText = (EditText) findViewById(R.id.edit_purse_total);
        mTypeSpinner = (Spinner) findViewById(R.id.spinner_type);
        mCurrencySpinner = (Spinner) findViewById(R.id.spinner_currency);

        // Get info from the intent that started the editor
        Intent intent = getIntent();
        mPurseUri = intent.getData();

        if (mPurseUri == null) {
            // Add a pet
            setTitle(R.string.editor_activity_title_new);
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Edit pet
            setTitle(R.string.editor_activity_title_edit);

            // Prepare the cursor loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }

        setupTypeSpinner();
        setupCurrencySpinner();

        mNameEditText.setOnTouchListener(mTouchListener);
        mTotalEditText.setOnTouchListener(mTouchListener);
        mTypeSpinner.setOnTouchListener(mTouchListener);
        mCurrencySpinner.setOnTouchListener(mTouchListener);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new purse, hide the "Delete" menu item.
        if (mPurseUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Check that the user has written at least the name
                String name = mNameEditText.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    // Insert new pet into the database
                    savePurse();
                }
                // Finish editing
                finish();
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Show a dialog to confirm
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!mPurseHasChanged) {
                    finish();
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                finish();
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the purse hasn't changed, continue with handling back button press
        if (!mPurseHasChanged){
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mPurseHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePurse();
                // Go back to the main activity
                NavUtils.navigateUpFromSameTask(EditorActivity.this);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the purse in the database.
     */
    private void deletePurse() {
        int rowsDeleted = 0;
        // Delete the pet associated with the current uri
        // Only perform the delete if this is an existing pet.
        if (mPurseUri != null) {
            // Call the ContentResolver to delete the pet at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentPetUri
            // content URI already identifies the pet that we want.
            rowsDeleted = getContentResolver().delete(mPurseUri, null, null);
        }

        if (rowsDeleted == 0) {
            Toast.makeText(this, R.string.editor_delete_purse_failed, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.editor_delete_purse_successful, Toast.LENGTH_SHORT).show();
        }

    }

    private void savePurse() {

        // Get the values from the data entered
        String name = mNameEditText.getText().toString().trim();
        int type = mType;
        int currency = mCurrency;
        int total;
        try {
            total = (int) (Float.parseFloat(mTotalEditText.getText().toString()) * 100);
        } catch (NumberFormatException e) {
            total = 0;
        }


        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(PurseEntry.COLUMN_NAME, name);
        values.put(PurseEntry.COLUMN_TOTAL, total);
        values.put(PurseEntry.COLUMN_TYPE, type);
        values.put(PurseEntry.COLUMN_CURRENCY, currency);

        Uri newUri = null;
        int rowsUpdated = 0;
        if (mPurseUri == null) {
            // Insert the new row, returning the primary key value of the new row
            newUri = getContentResolver().insert(PurseEntry.CONTENT_URI, values);
        } else {
            // Update the existing pet
            rowsUpdated = getContentResolver().update(mPurseUri, values, null, null);
        }

        //Show toast message
        if (newUri != null || rowsUpdated != 0) {
            Toast.makeText(this, R.string.editor_saved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.editor_not_saved, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i("HERE", "uri: " + mPurseUri);
        return new CursorLoader(this, mPurseUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor.moveToFirst()) {
            // Get data from the cursor
            String name = cursor.getString(cursor.getColumnIndex(PurseEntry.COLUMN_NAME));
            int total = cursor.getInt(cursor.getColumnIndex(PurseEntry.COLUMN_TOTAL));
            mType = cursor.getInt(cursor.getColumnIndex(PurseEntry.COLUMN_TYPE));
            mCurrency = cursor.getInt(cursor.getColumnIndex(PurseEntry.COLUMN_CURRENCY));

            // Set data on the UI
            mNameEditText.setText(name);
            mTotalEditText.setText(PurseEntry.formatTotal(total).replace(",", "."));
            mTypeSpinner.setSelection(mType);
            mCurrencySpinner.setSelection(mCurrency);
        }

        cursor.close();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clear out input fields
        mNameEditText.setText("");
        mTotalEditText.setText("");

        mType = PurseEntry.TYPE_CASH;
        mTypeSpinner.setSelection(mType);

        mCurrency = 0;
        mCurrencySpinner.setSelection(mCurrency);
    }

    /**
     * Setup the dropdown spinner that allows the user to select the type of the purse.
     */
    private void setupTypeSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter typeSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_type_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        typeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mTypeSpinner.setAdapter(typeSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.type_bank))) {
                        mType = PurseEntry.TYPE_BANK;
                    } else {
                        mType = PurseEntry.TYPE_CASH;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mType = PurseEntry.TYPE_CASH;
            }
        });
    }

    private void setupCurrencySpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter currencySpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_currency_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        currencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mCurrencySpinner.setAdapter(currencySpinnerAdapter);

        // Set the integer mSelected to the constant values
        mCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    mCurrency = position;
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrency = PurseEntry.EUR;
            }
        });
    }
}
