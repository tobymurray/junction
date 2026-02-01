/*
 * STUB: com.android.vcard.VCardInterpreter
 */
package com.android.vcard;

public interface VCardInterpreter {
    void onVCardStarted();
    void onVCardEnded();
    void onEntryStarted();
    void onEntryEnded();
    void onPropertyCreated(VCardProperty property);
}
