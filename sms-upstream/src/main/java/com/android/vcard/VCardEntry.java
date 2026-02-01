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
    }

    public static class OrganizationData {
        public String getOrganizationName() { return ""; }
        public String getTitle() { return ""; }
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

    public String getDisplayName() { return ""; }
    public List<PhoneData> getPhoneList() { return new ArrayList<>(); }
    public List<EmailData> getEmailList() { return new ArrayList<>(); }
    public List<PostalData> getPostalList() { return new ArrayList<>(); }
    public List<OrganizationData> getOrganizationList() { return new ArrayList<>(); }
    public List<ImData> getImList() { return new ArrayList<>(); }
    public List<WebsiteData> getWebsiteList() { return new ArrayList<>(); }
    public List<NoteData> getNotes() { return new ArrayList<>(); }
}
