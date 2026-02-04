/*
 * STUB: com.android.ex.photo.loaders.PhotoBitmapLoaderInterface
 */
package com.android.ex.photo.loaders;

import android.graphics.drawable.Drawable;

public interface PhotoBitmapLoaderInterface {

    void setPhotoUri(String photoUri);

    class BitmapResult {
        public static final int STATUS_SUCCESS = 0;
        public static final int STATUS_EXCEPTION = 1;

        public int status;
        public Drawable drawable;
    }
}
