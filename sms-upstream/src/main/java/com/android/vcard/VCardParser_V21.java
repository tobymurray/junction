/*
 * STUB: com.android.vcard.VCardParser_V21
 */
package com.android.vcard;

import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.io.InputStream;

public class VCardParser_V21 extends VCardParser {
    public VCardParser_V21() {}
    public VCardParser_V21(int vcardType) {}

    @Override
    public void parse(InputStream is) throws IOException, VCardException {
        // Stub - would parse V21 format vCard
    }

    @Override
    public void cancel() {}
}
