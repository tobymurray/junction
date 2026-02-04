/*
 * STUB: com.android.vcard.VCardEntry
 *
 * TODO: Replace with ez-vcard library or copy from AOSP frameworks/opt/vcard
 */
package com.android.vcard;

import android.accounts.Account;
import java.util.List;
import java.util.ArrayList;

public class VCardEntry {

    private int mVCardType;
    private Account mAccount;

    public VCardEntry() {
    }

    public VCardEntry(int vCardType, Account account) {
        mVCardType = vCardType;
        mAccount = account;
    }

    public static class PhoneData {
        public String getNumber() { return ""; }
        public int getType() { return 0; }
        public String getLabel() { return ""; }
        public boolean isPrimary() { return false; }
    }

    public static class EmailData {
        public String getAddress() { return ""; }
        public int getType() { return 0; }
        public String getLabel() { return ""; }
        public boolean isPrimary() { return false; }
    }

    public static class PostalData {
        public String getFormattedAddress(int type) { return ""; }
        public int getType() { return 0; }
        public String getLabel() { return ""; }
        public boolean isPrimary() { return false; }
        public String getPobox() { return ""; }
        public String getExtendedAddress() { return ""; }
        public String getStreet() { return ""; }
        public String getLocalty() { return ""; }
        public String getRegion() { return ""; }
        public String getPostalCode() { return ""; }
        public String getCountry() { return ""; }
    }

    public static class OrganizationData {
        public String getOrganizationName() { return ""; }
        public String getTitle() { return ""; }
        public int getType() { return 0; }
    }

    public static class ImData {
        public String getAddress() { return ""; }
        public int getProtocol() { return 0; }
        public String getCustomProtocol() { return ""; }
        public int getType() { return 0; }
    }

    public static class WebsiteData {
        public String getWebsite() { return ""; }
    }

    public static class NoteData {
        public String getNote() { return ""; }
    }

    public static class PhotoData {
        private byte[] mBytes;
        private String mFormat;
        private boolean mIsPrimary;

        public PhotoData(String format, byte[] bytes, boolean isPrimary) {
            mFormat = format;
            mBytes = bytes;
            mIsPrimary = isPrimary;
        }

        public byte[] getBytes() { return mBytes; }
        public String getFormat() { return mFormat; }
        public boolean isPrimary() { return mIsPrimary; }
    }

    public List<PhotoData> getPhotoList() { return new ArrayList<>(); }

    public void addProperty(VCardProperty property) {
        // Stub implementation
    }

    public void consolidateFields() {
        // Stub implementation
    }

    public String getDisplayName() { return ""; }
    public String getBirthday() { return null; }
    public List<PhoneData> getPhoneList() { return new ArrayList<>(); }
    public List<EmailData> getEmailList() { return new ArrayList<>(); }
    public List<PostalData> getPostalList() { return new ArrayList<>(); }
    public List<OrganizationData> getOrganizationList() { return new ArrayList<>(); }
    public List<ImData> getImList() { return new ArrayList<>(); }
    public List<WebsiteData> getWebsiteList() { return new ArrayList<>(); }
    public List<NoteData> getNotes() { return new ArrayList<>(); }
}
