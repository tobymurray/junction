/*
 * STUB: com.android.ex.chips.RecipientEntry
 *
 * This is a stub class to allow compilation. The real implementation
 * is from AOSP's chips library (platform/frameworks/ex/chips).
 *
 * TODO: Either:
 * 1. Copy the real implementation from AOSP
 * 2. Replace with Material chips or another contact picker library
 */
package com.android.ex.chips;

import android.net.Uri;

public class RecipientEntry {

    public static final int ENTRY_TYPE_PERSON = 0;
    public static final int ENTRY_TYPE_PERMISSION_REQUEST = 1;
    public static final int ENTRY_TYPE_GENERATE = 2;

    public static final int INVALID_CONTACT = -1;
    public static final int GENERATED_CONTACT = -2;

    private long mContactId;
    private String mDisplayName;
    private String mDestination;
    private int mDestinationType;
    private String mDestinationLabel;
    private Uri mPhotoThumbnailUri;
    private int mEntryType;
    private boolean mIsFirstLevel;
    private boolean mIsValid;
    private long mDataId;
    private String mLookupKey;

    private RecipientEntry(int entryType, String displayName, String destination,
            int destinationType, String destinationLabel, long contactId,
            Long directoryId, long dataId, Uri photoThumbnailUri,
            boolean isFirstLevel, boolean isValid, String lookupKey) {
        mEntryType = entryType;
        mDisplayName = displayName;
        mDestination = destination;
        mDestinationType = destinationType;
        mDestinationLabel = destinationLabel;
        mContactId = contactId;
        mDataId = dataId;
        mPhotoThumbnailUri = photoThumbnailUri;
        mIsFirstLevel = isFirstLevel;
        mIsValid = isValid;
        mLookupKey = lookupKey;
    }

    public static RecipientEntry constructFakeEntry(final String address, final boolean isValid) {
        return new RecipientEntry(ENTRY_TYPE_PERSON, address, address, 0, null,
                INVALID_CONTACT, null, INVALID_CONTACT, null, true, isValid, null);
    }

    public static RecipientEntry constructFakePhoneEntry(final String phoneNumber,
            final boolean isValid) {
        return constructFakeEntry(phoneNumber, isValid);
    }

    public static RecipientEntry constructTopLevelEntry(String displayName, int displayNameSource,
            String destination, int destinationType, String destinationLabel, long contactId,
            Long directoryId, long dataId, Uri photoThumbnailUri, boolean isValid,
            String lookupKey) {
        return new RecipientEntry(ENTRY_TYPE_PERSON, displayName, destination,
                destinationType, destinationLabel, contactId, directoryId, dataId,
                photoThumbnailUri, true, isValid, lookupKey);
    }

    public static RecipientEntry constructSecondLevelEntry(String displayName, int displayNameSource,
            String destination, int destinationType, String destinationLabel, long contactId,
            Long directoryId, long dataId, Uri photoThumbnailUri, boolean isValid,
            String lookupKey) {
        return new RecipientEntry(ENTRY_TYPE_PERSON, displayName, destination,
                destinationType, destinationLabel, contactId, directoryId, dataId,
                photoThumbnailUri, false, isValid, lookupKey);
    }

    public int getEntryType() { return mEntryType; }
    public String getDisplayName() { return mDisplayName; }
    public String getDestination() { return mDestination; }
    public int getDestinationType() { return mDestinationType; }
    public String getDestinationLabel() { return mDestinationLabel; }
    public long getContactId() { return mContactId; }
    public long getDataId() { return mDataId; }
    public Uri getPhotoThumbnailUri() { return mPhotoThumbnailUri; }
    public boolean isFirstLevel() { return mIsFirstLevel; }
    public boolean isValid() { return mIsValid; }
    public String getLookupKey() { return mLookupKey; }
    public boolean isSelectable() { return mEntryType == ENTRY_TYPE_PERSON; }
}
