/*
 * STUB: com.android.ex.chips.RecipientEditTextView
 *
 * This is a stub class to allow compilation.
 *
 * TODO: Replace with proper implementation or alternative library.
 */
package com.android.ex.chips;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.util.ArrayList;
import java.util.List;

public class RecipientEditTextView extends MultiAutoCompleteTextView
        implements TextView.OnEditorActionListener {

    public interface RecipientChipDeletedListener {
        void onRecipientChipDeleted(DrawableRecipientChip chip);
    }

    public interface RecipientChipAddedListener {
        void onRecipientChipAdded(DrawableRecipientChip chip);
    }

    public RecipientEditTextView(Context context) {
        super(context);
        setOnEditorActionListener(this);
    }

    public RecipientEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnEditorActionListener(this);
    }

    public RecipientEditTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnEditorActionListener(this);
    }

    public void setTokenizer(Tokenizer tokenizer) {
        super.setTokenizer(tokenizer);
    }

    public void setAdapter(BaseRecipientAdapter adapter) {
        super.setAdapter(adapter);
    }

    public void setRecipientChipDeletedListener(RecipientChipDeletedListener listener) {
        // Stub
    }

    public void setRecipientChipAddedListener(RecipientChipAddedListener listener) {
        // Stub
    }

    public DrawableRecipientChip[] getSortedRecipients() {
        return new DrawableRecipientChip[0];
    }

    public DrawableRecipientChip[] getRecipients() {
        return new DrawableRecipientChip[0];
    }

    public void removeRecipientChip(DrawableRecipientChip chip) {
        // Stub
    }

    public void removeRecipientEntry(RecipientEntry entry) {
        // Stub - removes a recipient by entry
    }

    public void replaceChip(DrawableRecipientChip chip, RecipientEntry entry) {
        // Stub
    }

    public void appendRecipientEntry(RecipientEntry entry) {
        // Stub
    }

    public void clearSelectedChip() {
        // Stub
    }

    public int getRecipientCount() {
        return 0;
    }

    public void setChipDimensions(float size, float spacing) {
        // Stub
    }

    public boolean isPhoneQuery() {
        return true;
    }

    public void setDropdownChipLayouter(DropdownChipLayouter layouter) {
        // Stub
    }

    /**
     * Controls whether recipients list shrinks when focus is lost.
     */
    public void setOnFocusListShrinkRecipients(boolean shrinkOnFocusLoss) {
        // Stub
    }

    /**
     * Called when an editor action occurs on this view.
     * Subclasses can override this to handle the action.
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // Default implementation does nothing - subclasses may override
        return false;
    }
}
