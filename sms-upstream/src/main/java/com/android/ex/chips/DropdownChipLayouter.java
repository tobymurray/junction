/*
 * STUB: com.android.ex.chips.DropdownChipLayouter
 *
 * This is a stub class to allow compilation.
 */
package com.android.ex.chips;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DropdownChipLayouter {

    public enum AdapterType {
        BASE_RECIPIENT,
        RECIPIENT_ALTERNATES,
        SINGLE_RECIPIENT
    }

    private LayoutInflater mInflater;

    public DropdownChipLayouter(LayoutInflater inflater, Context context) {
        mInflater = inflater;
    }

    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry,
            int position, AdapterType type, String constraint,
            StateListDrawableSpan deleteDrawable) {
        return convertView;
    }

    public View newView(LayoutInflater inflater, ViewGroup parent, AdapterType type) {
        return null;
    }

    public static class StateListDrawableSpan {
    }
}
