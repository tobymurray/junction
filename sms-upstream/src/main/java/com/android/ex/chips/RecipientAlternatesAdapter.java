/*
 * STUB: com.android.ex.chips.RecipientAlternatesAdapter
 *
 * This is a stub class to allow compilation.
 */
package com.android.ex.chips;

import android.content.Context;
import android.database.Cursor;
import android.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipientAlternatesAdapter extends CursorAdapter {

    /** Maximum number of lookups to perform in a single batch. */
    public static final int MAX_LOOKUPS = 50;

    public interface RecipientMatchCallback {
        void matchesFound(Map<String, RecipientEntry> results);
        void matchesNotFound(List<RecipientEntry> entries);
    }

    public RecipientAlternatesAdapter(Context context, long contactId, Long directoryId,
            String lookupKey, long dataId, int queryType, OnCheckedItemChangedListener listener) {
        super(context, null, false);
    }

    public interface OnCheckedItemChangedListener {
        void onCheckedItemChanged(int position);
    }

    public static void getMatchingRecipients(Context context, BaseRecipientAdapter adapter,
            ArrayList<String> addresses, int queryType, RecipientMatchCallback callback) {
        // Stub - call matchesNotFound with empty list
        if (callback != null) {
            callback.matchesNotFound(new ArrayList<RecipientEntry>());
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    }
}
