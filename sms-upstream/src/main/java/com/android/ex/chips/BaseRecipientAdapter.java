/*
 * STUB: com.android.ex.chips.BaseRecipientAdapter
 *
 * This is a stub class to allow compilation.
 *
 * TODO: Replace with proper implementation or alternative library.
 */
package com.android.ex.chips;

import android.content.Context;
import android.database.Cursor;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class BaseRecipientAdapter extends BaseAdapter implements Filterable {

    public static final int QUERY_TYPE_PHONE = 0;
    public static final int QUERY_TYPE_EMAIL = 1;

    protected Context mContext;
    protected int mQueryType;
    protected int mPreferredMaxResultCount;
    protected CharSequence mCurrentConstraint;
    protected List<RecipientEntry> mEntries = new ArrayList<>();
    protected Object mPhotoManager;

    public BaseRecipientAdapter(Context context) {
        mContext = context;
    }

    public BaseRecipientAdapter(Context context, int queryType) {
        mContext = context;
        mQueryType = queryType;
    }

    public BaseRecipientAdapter(Context context, int preferredMaxResultCount, int queryType) {
        mContext = context;
        mPreferredMaxResultCount = preferredMaxResultCount;
        mQueryType = queryType;
    }

    public void setQueryType(int queryType) {
        mQueryType = queryType;
    }

    public int getQueryType() {
        return mQueryType;
    }

    public Context getContext() {
        return mContext;
    }

    public void setPhotoManager(Object photoManager) {
        mPhotoManager = photoManager;
    }

    /**
     * Override this to force the adapter to always show the address.
     */
    public boolean forceShowAddress() {
        return false;
    }

    /**
     * Called to get matching recipients for address substitution.
     */
    public void getMatchingRecipients(ArrayList<String> addresses,
            RecipientAlternatesAdapter.RecipientMatchCallback callback) {
        // Default implementation - subclasses should override
        if (callback != null) {
            callback.matchesNotFound(new ArrayList<RecipientEntry>());
        }
    }

    /**
     * Clear any temporary entries.
     */
    protected void clearTempEntries() {
        // Stub - subclasses may override
    }

    /**
     * Update the adapter with new entries.
     */
    protected void updateEntries(List<RecipientEntry> entries) {
        mEntries.clear();
        if (entries != null) {
            mEntries.addAll(entries);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public RecipientEntry getItem(int position) {
        if (position >= 0 && position < mEntries.size()) {
            return mEntries.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        RecipientEntry entry = getItem(position);
        if (entry != null) {
            return entry.getEntryType();
        }
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
            }
        };
    }

    public void setAccount(android.accounts.Account account) {
        // Stub
    }

    public void setShowMobileOnly(boolean showMobileOnly) {
        // Stub
    }
}
