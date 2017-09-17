package com.example.laura.mymoney;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.laura.mymoney.data.PurseContract.PurseEntry;
import com.example.laura.mymoney.data.TransactionContract.TransactionEntry;

import java.io.File;
import java.io.IOException;
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

    private static final int DEFAULT_TRANS_TYPE = TransactionEntry.TYPE_NEGATIVE;
    private static final int LOADER_ID = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

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
    ImageView mImageView;

    String mCurrentPhotoPath = "";

    // TODO: Implement bank add from mail

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        // Get default purse for shortcut addition
        SharedPreferences sharedPreferences = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        int defaultPurse = sharedPreferences.getInt(getString(R.string.default_purse_preferences), -1);

        // Get info from the intent that started the editor
        Intent intent = getIntent();
        mPurseId = intent.getIntExtra("PURSE_ID", defaultPurse);
        mTransType = intent.getIntExtra("TYPE", DEFAULT_TRANS_TYPE);
        mPurseUri = ContentUris.withAppendedId(PurseEntry.CONTENT_URI, mPurseId);

        // Get views from layout
        mDateEditText = (EditText) findViewById(R.id.transaction_date);
        mTimeEditText = (EditText) findViewById(R.id.transaction_time);
        mConceptEditText = (EditText) findViewById(R.id.transaction_concept);
        mPlaceEditText = (EditText) findViewById(R.id.transaction_place);
        mAmountEditText = (EditText) findViewById(R.id.transaction_amount);
        mCurrencyTextView = (TextView) findViewById(R.id.transaction_currency);
        mPhotoButton = (Button) findViewById(R.id.add_receipt_button);
        mImageView = (ImageView) findViewById(R.id.receipt_image);

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
                if (mPhotoButton.getText() == getString(R.string.add_receipt)) {
                    // https://developer.android.com/training/camera/photobasics.html
                    dispatchTakePictureIntent();
                } else if (mPhotoButton.getText() == getString(R.string.delete_receipt)) {
                    // Reset image
                    File photoFile = new File(mCurrentPhotoPath);
                    boolean deleted = photoFile.delete();
                    if (deleted) {
                        Toast.makeText(TransactionActivity.this, R.string.photo_delete_successful,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TransactionActivity.this, R.string.photo_delete_failed,
                                Toast.LENGTH_SHORT).show();
                    }

                    mImageView.setImageBitmap(null);
                    mCurrentPhotoPath = "";

                    // Reset button
                    mPhotoButton.setText(R.string.add_receipt);
                    mPhotoButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_photo, 0, 0, 0);
                }
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
                Log.i("action_save", "finish()");
                finish();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the purse hasn't changed, continue with handling back button press
                if (!transactionHasContent()){
                    Log.i("onBackPressed", "super");
                    Intent intent = new Intent(TransactionActivity.this, HistoryActivity.class);
                    intent.setData(mPurseUri);
                    startActivity(intent);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, delete image if taken and
                                // close the current activity.
                                if (!mCurrentPhotoPath.equals("")) {
                                    Log.i("onBackPressed", "hay foto");
                                    File photoFile = new File(mCurrentPhotoPath);
                                    photoFile.delete();
                                    mCurrentPhotoPath = "";
                                }
                                Log.i("onBackPressed", "salir");
                                Intent intent = new Intent(TransactionActivity.this, HistoryActivity.class);
                                intent.setData(mPurseUri);
                                startActivity(intent);
                            }
                        };

                // Show dialog that there are unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the purse hasn't changed, continue with handling back button press
        if (!transactionHasContent()){
            Log.i("onBackPressed", "super");
            Intent intent = new Intent(TransactionActivity.this, HistoryActivity.class);
            intent.setData(mPurseUri);
            startActivity(intent);
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, delete image if taken and
                        // close the current activity.
                        if (!mCurrentPhotoPath.equals("")) {
                            Log.i("onBackPressed", "hay foto");
                            File photoFile = new File(mCurrentPhotoPath);
                            photoFile.delete();
                            mCurrentPhotoPath = "";
                        }
                        Log.i("onBackPressed", "salir");
                        Intent intent = new Intent(TransactionActivity.this, HistoryActivity.class);
                        intent.setData(mPurseUri);
                        startActivity(intent);
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Show image
            Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
            mImageView.setImageBitmap(imageBitmap);

            // Change button to delete
            mPhotoButton.setText(R.string.delete_receipt);
            mPhotoButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete_black, 0, 0, 0);

            //TODO: Doesn't work
            galleryAddPic();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            // Clear photo path so that it is not saved to database
            mCurrentPhotoPath = "";
        }
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
        transValues.put(TransactionEntry.COLUMN_RECEIPT, mCurrentPhotoPath);

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

    private void setDatePicker () {
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

    private void setTimePicker () {
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

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("TransactionActivity", "Error while creating file for image");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.laura.mymoney.fileprovider", photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private boolean transactionHasContent () {

        boolean hasContent = !mCurrentPhotoPath.equals("");
        hasContent = hasContent || !mConceptEditText.getText().toString().equals("");
        hasContent = hasContent || !mPlaceEditText.getText().toString().equals("");
        hasContent = hasContent || !mAmountEditText.getText().toString().equals("");

        return hasContent;
    }

}
