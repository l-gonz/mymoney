package com.example.laura.mymoney.data;

import android.content.ContentResolver;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Laura on 2/9/17.
 */

public class TransactionContract {

    // URI related constants
    public static final String CONTENT_AUTHORITY = "com.example.laura.mymoney";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_TRANSACTIONS = "transactions";

    public static abstract class TransactionEntry implements BaseColumns {

        public static final String TABLE_NAME = "transactions";


        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_CONCEPT = "concept";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_PLACE = "place";
        public static final String COLUMN_PURSE = "purse";
        public static final String COLUMN_TOTAL = "total";
        public static final String COLUMN_CURRENCY = "currency";
        public static final String COLUMN_RECEIPT = "receipt";

        //Constants for type options
        public static final int TYPE_NEGATIVE = 0;
        public static final int TYPE_POSITIVE = 1;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_TRANSACTIONS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of entries.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTIONS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single entry.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTIONS;

        public static boolean isTypeValid (int type) {
            return type == TYPE_NEGATIVE || type == TYPE_POSITIVE;
        }

        public static String formatDate (long date) {
            return new SimpleDateFormat("dd/MM/yyyy").format(date * 1000);
        }

        public static String formatDateLong (Calendar calendar) {
            return new SimpleDateFormat("MMMM yyyy").format(calendar);
        }

    }
}
