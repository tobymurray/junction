/*
 * STUB: com.android.ex.photo.PhotoViewCallbacks
 */
package com.android.ex.photo;

import androidx.fragment.app.Fragment;

public interface PhotoViewCallbacks {

    int BITMAP_LOADER_AVATAR = PhotoViewController.BITMAP_LOADER_AVATAR;
    int BITMAP_LOADER_THUMBNAIL = PhotoViewController.BITMAP_LOADER_THUMBNAIL;
    int BITMAP_LOADER_PHOTO = PhotoViewController.BITMAP_LOADER_PHOTO;

    boolean isFragmentActive(Fragment fragment);
}
