/*
 * STUB: com.android.vcard.VCardParser
 */
package com.android.vcard;

import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.io.InputStream;

public abstract class VCardParser {
    protected VCardInterpreter mInterpreter;

    public void addInterpreter(VCardInterpreter interpreter) {
        mInterpreter = interpreter;
    }

    /**
     * Parse the vCard from the input stream.
     */
    public abstract void parse(InputStream is) throws IOException, VCardException;

    public abstract void cancel();
}
