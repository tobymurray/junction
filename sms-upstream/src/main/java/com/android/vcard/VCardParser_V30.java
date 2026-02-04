/*
 * STUB: com.android.vcard.VCardParser_V30
 */
package com.android.vcard;

import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.io.InputStream;

public class VCardParser_V30 extends VCardParser {
    public VCardParser_V30() {}
    public VCardParser_V30(int vcardType) {}

    @Override
    public void parse(InputStream is) throws IOException, VCardException {
        // Stub - would parse V30 format vCard
    }

    @Override
    public void cancel() {}
}
