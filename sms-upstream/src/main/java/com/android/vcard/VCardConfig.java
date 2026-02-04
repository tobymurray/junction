/*
 * STUB: com.android.vcard.VCardConfig
 */
package com.android.vcard;

public class VCardConfig {
    public static final int VCARD_TYPE_UNKNOWN = 0x0000000;
    public static final int VCARD_TYPE_V21_GENERIC = 0x0000001;
    public static final int VCARD_TYPE_V30_GENERIC = 0x0000002;
    public static final int VCARD_TYPE_V40_GENERIC = 0x0000004;
    public static final int VCARD_TYPE_DEFAULT = VCARD_TYPE_V21_GENERIC;

    public static int getVCardTypeFromString(String vcardTypeStr) {
        if (vcardTypeStr != null) {
            if (vcardTypeStr.contains("3.0")) return VCARD_TYPE_V30_GENERIC;
            if (vcardTypeStr.contains("4.0")) return VCARD_TYPE_V40_GENERIC;
        }
        return VCARD_TYPE_V21_GENERIC;
    }
}
