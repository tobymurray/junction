/*
 * STUB: com.android.ex.photo.loaders.PhotoBitmapLoaderInterface
 */
package com.android.ex.photo.loaders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface PhotoBitmapLoaderInterface {

    class BitmapResult {
        public Bitmap bitmap;
        public Drawable placeholder;
        public int status;

        public static final int STATUS_SUCCESS = 0;
        public static final int STATUS_EXCEPTION = 1;
    }
}
