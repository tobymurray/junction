/*
 * STUB: com.android.ex.photo.PhotoViewCallbacks
 */
package com.android.ex.photo;

import android.database.Cursor;

public interface PhotoViewCallbacks {
    void onFragmentVisible(PhotoViewFragment fragment);
    void addScreenListener(int position, OnScreenListener listener);
    void removeScreenListener(int position);
    void addCursorListener(CursorChangedListener listener);
    void removeCursorListener(CursorChangedListener listener);
    boolean isFragmentActive(PhotoViewFragment fragment);
    void onNewPhotoLoaded(int position);
    void toggleFullScreen();
    boolean isFragmentFullScreen(PhotoViewFragment fragment);

    interface OnScreenListener {
        void onFullScreenChanged(boolean fullScreen);
        void onViewUpNext();
        void onFragmentPhotoLoadComplete(PhotoViewFragment fragment, boolean success);
    }

    interface CursorChangedListener {
        void onCursorChanged(Cursor cursor);
    }
}
