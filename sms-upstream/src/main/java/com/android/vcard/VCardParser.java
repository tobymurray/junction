/*
 * STUB: com.android.vcard.VCardParser
 */
package com.android.vcard;

import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.io.InputStream;

public abstract class VCardParser {
    public abstract void parse(InputStream is, VCardInterpreter interpreter)
            throws IOException, VCardException;
    public abstract void cancel();
}
