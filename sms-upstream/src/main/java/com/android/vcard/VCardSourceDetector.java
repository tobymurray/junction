/*
 * STUB: com.android.vcard.VCardSourceDetector
 */
package com.android.vcard;

public class VCardSourceDetector implements VCardInterpreter {
    private int mEstimatedType = VCardConfig.VCARD_TYPE_V21_GENERIC;

    public int getEstimatedType() { return mEstimatedType; }

    @Override
    public void onVCardStarted() {}

    @Override
    public void onVCardEnded() {}

    @Override
    public void onEntryStarted() {}

    @Override
    public void onEntryEnded() {}

    @Override
    public void onPropertyCreated(VCardProperty property) {
        // Stub - detect version from property
    }
}
