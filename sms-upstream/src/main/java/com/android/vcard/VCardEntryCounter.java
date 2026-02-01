/*
 * STUB: com.android.vcard.VCardEntryCounter
 */
package com.android.vcard;

public class VCardEntryCounter implements VCardInterpreter {
    private int mCount = 0;

    public int getCount() { return mCount; }

    @Override
    public void onVCardStarted() {}

    @Override
    public void onVCardEnded() {}

    @Override
    public void onEntryStarted() {}

    @Override
    public void onEntryEnded() { mCount++; }

    @Override
    public void onPropertyCreated(VCardProperty property) {}
}
