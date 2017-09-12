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

/**
 * Created by Laura on 26/8/17.
 */

public class PurseCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link PurseCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public PurseCursorAdapter(Context context, Cursor c) {
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
        return LayoutInflater.from(context).inflate(R.layout.purse_item, parent, false);
    }

    /**
     * This method binds the pet data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current pet can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView nameView = (TextView) view.findViewById(R.id.purse_name);
        TextView totalView = (TextView) view.findViewById(R.id.purse_total);

        // Extract properties from cursor
        String name = cursor.getString(cursor.getColumnIndexOrThrow(PurseEntry.COLUMN_NAME));
        int totalNum = cursor.getInt(cursor.getColumnIndexOrThrow(PurseEntry.COLUMN_TOTAL));
        int currency = cursor.getInt(cursor.getColumnIndexOrThrow(PurseEntry.COLUMN_CURRENCY));

        // Format currency
        String totalText = PurseEntry.formatTotal(totalNum);
        totalText += PurseEntry.selectCurrencySymbol(currency);

        // Populate fields with extracted properties
        nameView.setText(name);
        totalView.setText(totalText);
    }

}
