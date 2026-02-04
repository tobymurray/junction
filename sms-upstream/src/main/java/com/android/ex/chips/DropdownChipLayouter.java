/*
 * STUB: com.android.ex.chips.DropdownChipLayouter
 *
 * This is a stub class to allow compilation.
 */
package com.android.ex.chips;

import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class DropdownChipLayouter {

    public enum AdapterType {
        BASE_RECIPIENT,
        RECIPIENT_ALTERNATES,
        SINGLE_RECIPIENT
    }

    private LayoutInflater mInflater;
    private Context mContext;

    public DropdownChipLayouter(LayoutInflater inflater, Context context) {
        mInflater = inflater;
        mContext = context;
    }

    protected LayoutInflater getInflater() {
        return mInflater;
    }

    protected Context getContext() {
        return mContext;
    }

    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry,
            int position, AdapterType type, String substring,
            StateListDrawable deleteDrawable) {
        return convertView;
    }

    protected void bindIconToView(boolean showImage, RecipientEntry entry, ImageView view,
            AdapterType type) {
        // Stub implementation
    }

    protected CharSequence[] getStyledResults(String substring, String displayName,
            String destination) {
        return new CharSequence[] { displayName, destination };
    }

    protected View reuseOrInflateView(View convertView, ViewGroup parent, AdapterType type) {
        if (convertView != null) {
            return convertView;
        }
        return mInflater.inflate(getItemLayoutResId(type), parent, false);
    }

    protected int getItemLayoutResId(AdapterType type) {
        return 0;
    }

    protected int getAlternateItemLayoutResId(AdapterType type) {
        return 0;
    }

    public View newView(LayoutInflater inflater, ViewGroup parent, AdapterType type) {
        return null;
    }
}
