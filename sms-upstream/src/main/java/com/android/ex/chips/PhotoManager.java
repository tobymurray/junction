/*
 * STUB: com.android.ex.chips.PhotoManager
 *
 * This is a stub interface to allow compilation.
 */
package com.android.ex.chips;

public interface PhotoManager {

    interface PhotoManagerCallback {
        void onPhotoBytesPopulated();
        void onPhotoBytesAsyncLoadFailed();
        void onPhotoBytesAsynchronouslyPopulated();
    }

    void populatePhotoBytesAsync(RecipientEntry entry, PhotoManagerCallback callback);
}
