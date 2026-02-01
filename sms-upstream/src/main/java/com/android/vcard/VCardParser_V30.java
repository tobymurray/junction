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
    public void parse(InputStream is, VCardInterpreter interpreter)
            throws IOException, VCardException {
        // Stub
    }

    @Override
    public void cancel() {}
}
