/*
 * STUB: com.android.ex.chips.recipientchip.DrawableRecipientChip
 *
 * This is a stub class to allow compilation.
 *
 * TODO: Replace with proper implementation or alternative library.
 */
package com.android.ex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import com.android.ex.chips.RecipientEntry;

public class DrawableRecipientChip extends ImageSpan {

    private RecipientEntry mEntry;
    private boolean mSelected;

    public DrawableRecipientChip(Drawable drawable, RecipientEntry entry) {
        super(drawable);
        mEntry = entry;
    }

    public RecipientEntry getEntry() {
        return mEntry;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public CharSequence getDisplay() {
        return mEntry != null ? mEntry.getDisplayName() : "";
    }

    public CharSequence getValue() {
        return mEntry != null ? mEntry.getDestination() : "";
    }

    public long getContactId() {
        return mEntry != null ? mEntry.getContactId() : -1;
    }

    public Long getDirectoryId() {
        return null;
    }

    public long getDataId() {
        return mEntry != null ? mEntry.getDataId() : -1;
    }

    public Rect getBounds() {
        Drawable d = getDrawable();
        if (d != null) {
            return d.getBounds();
        }
        return new Rect();
    }

    public void draw(Canvas canvas, CharSequence text, int start, int end,
            float x, int top, int y, int bottom, android.graphics.Paint paint) {
        // Stub
    }
}
