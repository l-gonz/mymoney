package com.example.laura.mymoney.data;

import android.content.ContentResolver;
import android.icu.text.DecimalFormat;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Laura on 11/6/17.
 */

public final class PurseContract {

    // URI related constants
    public static final String CONTENT_AUTHORITY = "com.example.laura.mymoney";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PURSES = "purses";

    public static abstract class PurseEntry implements BaseColumns {

        public static final String TABLE_NAME = "purses";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TOTAL = "total";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_CURRENCY = "currency";

        //Constants for type options
        public static final int TYPE_CASH = 0;
        public static final int TYPE_BANK = 1;

        //Constants for currency options
        public static final int EUR = 0;
        public static final int GBP = 1;
        public static final int NOK = 2;
        public static final int TRY = 3;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PURSES);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of pets.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PURSES;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single pet.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PURSES;

        public static String formatTotal (int total) {
            DecimalFormat formatter = new DecimalFormat("#0.00");
            return formatter.format((float)total / 100);
        }

        public static boolean isTypeValid (int type) {
            return type == TYPE_CASH || type == TYPE_BANK;
        }

        public static boolean isCurrencyValid (int c) {
            return c >= EUR || c <= TRY;
        }

        public static String selectCurrencySymbol(int c) {
            String symbol = " -";
            switch (c) {
                case EUR:
                    symbol = " €";
                    break;
                case GBP:
                    symbol = " £";
                    break;
                case NOK:
                    symbol = " kr";
                    break;
                case TRY:
                    symbol = " ₺";
                    break;
            }
            return symbol;
        }

    }
}
