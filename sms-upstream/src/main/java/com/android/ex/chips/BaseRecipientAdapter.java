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

public class BaseRecipientAdapter extends BaseAdapter implements Filterable {

    public static final int QUERY_TYPE_PHONE = 0;
    public static final int QUERY_TYPE_EMAIL = 1;

    protected Context mContext;
    protected int mQueryType;

    public BaseRecipientAdapter(Context context) {
        mContext = context;
    }

    public BaseRecipientAdapter(Context context, int queryType) {
        mContext = context;
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

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public RecipientEntry getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
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
