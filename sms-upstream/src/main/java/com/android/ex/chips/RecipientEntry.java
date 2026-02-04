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
    // Total number of entry types (used for view type count in adapters)
    public static final int ENTRY_TYPE_SIZE = 3;

    public static final int INVALID_CONTACT = -1;
    public static final int GENERATED_CONTACT = -2;
    public static final int INVALID_DESTINATION_TYPE = -1;

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
    private Long mDirectoryId;
    private byte[] mPhotoBytes;

    protected RecipientEntry(int entryType, String displayName, String destination,
            int destinationType, String destinationLabel, long contactId,
            Long directoryId, long dataId, Uri photoThumbnailUri,
            boolean isFirstLevel, boolean isValid, String lookupKey) {
        mEntryType = entryType;
        mDisplayName = displayName;
        mDestination = destination;
        mDestinationType = destinationType;
        mDestinationLabel = destinationLabel;
        mContactId = contactId;
        mDirectoryId = directoryId;
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

    // Overload accepting String photoThumbnailUri
    public static RecipientEntry constructTopLevelEntry(String displayName, int displayNameSource,
            String destination, int destinationType, String destinationLabel, long contactId,
            Long directoryId, long dataId, String photoThumbnailUri, boolean isValid,
            String lookupKey) {
        Uri uri = photoThumbnailUri != null ? Uri.parse(photoThumbnailUri) : null;
        return new RecipientEntry(ENTRY_TYPE_PERSON, displayName, destination,
                destinationType, destinationLabel, contactId, directoryId, dataId,
                uri, true, isValid, lookupKey);
    }

    public static RecipientEntry constructSecondLevelEntry(String displayName, int displayNameSource,
            String destination, int destinationType, String destinationLabel, long contactId,
            Long directoryId, long dataId, Uri photoThumbnailUri, boolean isValid,
            String lookupKey) {
        return new RecipientEntry(ENTRY_TYPE_PERSON, displayName, destination,
                destinationType, destinationLabel, contactId, directoryId, dataId,
                photoThumbnailUri, false, isValid, lookupKey);
    }

    // Overload accepting String photoThumbnailUri
    public static RecipientEntry constructSecondLevelEntry(String displayName, int displayNameSource,
            String destination, int destinationType, String destinationLabel, long contactId,
            Long directoryId, long dataId, String photoThumbnailUri, boolean isValid,
            String lookupKey) {
        Uri uri = photoThumbnailUri != null ? Uri.parse(photoThumbnailUri) : null;
        return new RecipientEntry(ENTRY_TYPE_PERSON, displayName, destination,
                destinationType, destinationLabel, contactId, directoryId, dataId,
                uri, false, isValid, lookupKey);
    }

    /**
     * Checks if the contact ID indicates a recipient that was created (not from contacts).
     */
    public static boolean isCreatedRecipient(long contactId) {
        return contactId == INVALID_CONTACT || contactId == GENERATED_CONTACT;
    }

    public int getEntryType() { return mEntryType; }
    public String getDisplayName() { return mDisplayName; }
    public String getDestination() { return mDestination; }
    public int getDestinationType() { return mDestinationType; }
    public String getDestinationLabel() { return mDestinationLabel; }
    public long getContactId() { return mContactId; }
    public Long getDirectoryId() { return mDirectoryId; }
    public long getDataId() { return mDataId; }
    public Uri getPhotoThumbnailUri() { return mPhotoThumbnailUri; }
    public boolean isFirstLevel() { return mIsFirstLevel; }
    public boolean isValid() { return mIsValid; }
    public String getLookupKey() { return mLookupKey; }
    public boolean isSelectable() { return mEntryType == ENTRY_TYPE_PERSON; }

    public byte[] getPhotoBytes() { return mPhotoBytes; }
    public void setPhotoBytes(byte[] photoBytes) { mPhotoBytes = photoBytes; }

    /**
     * Determines if this entry represents the same person as another entry.
     * Subclasses may override this to provide different comparison logic.
     */
    public boolean isSamePerson(RecipientEntry entry) {
        return mContactId == entry.mContactId && mContactId != INVALID_CONTACT;
    }
}
