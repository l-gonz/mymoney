package com.example.laura.mymoney.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.laura.mymoney.R;
import com.example.laura.mymoney.data.PurseContract.PurseEntry;
import com.example.laura.mymoney.data.TransactionContract.TransactionEntry;

/**
 * Created by Laura on 26/8/17.
 */

public class TransactionCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link TransactionCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public TransactionCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
    }

    /**
     * This method binds the transaction data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView dateView = (TextView) view. findViewById(R.id.date_text_view);
        TextView conceptView = (TextView) view.findViewById(R.id.concept_text_view);
        TextView quantityView = (TextView) view.findViewById(R.id.quantity_text_view);
        TextView totalView = (TextView) view.findViewById(R.id.total_after_text_view);

        // Extract properties from cursor
        String concept = cursor.getString(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_CONCEPT));
        long date = cursor.getLong(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_DATE));
        int amount = cursor.getInt(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_AMOUNT));
        int type = cursor.getInt(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_TYPE));
        int total = cursor.getInt(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_TOTAL));
        int currency = cursor.getInt(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_CURRENCY));

        // Format with currency
        String quantityText = PurseEntry.formatTotal(amount);
        quantityText += PurseEntry.selectCurrencySymbol(currency);
        String totalText = PurseEntry.formatTotal(total);
        totalText += PurseEntry.selectCurrencySymbol(currency);

        // Increase vs decrease total
        if (type == TransactionEntry.TYPE_NEGATIVE) {
            quantityText = "- " + quantityText;
            quantityView.setTextColor(view.getResources().getColor(R.color.red, null));
        } else if (type == TransactionEntry.TYPE_POSITIVE) {
            quantityText = "+ " + quantityText;
            quantityView.setTextColor(view.getResources().getColor(R.color.green, null));
        }

        // Populate fields with extracted properties
        conceptView.setText(concept);
        dateView.setText(TransactionEntry.formatDate(date));
        quantityView.setText(quantityText);
        totalView.setText(totalText);

    }

}
