/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ex.photo;

import android.content.Context;
import android.content.Intent;

public class Intents {

    // Intent extra keys
    public static final String EXTRA_PHOTOS_URI = "photos_uri";
    public static final String EXTRA_INITIAL_PHOTO_URI = "initial_photo_uri";
    public static final String EXTRA_PROJECTION = "projection";
    public static final String EXTRA_MAX_INITIAL_SCALE = "max_initial_scale";
    public static final String EXTRA_DISPLAY_THUMBS_FULLSCREEN = "display_thumbs_fullscreen";

    public static PhotoViewIntentBuilder newPhotoViewIntentBuilder(
            Context context, Class<?> activityClass) {
        return new PhotoViewIntentBuilder(context, activityClass);
    }

    public static class PhotoViewIntentBuilder {

        private final Context mContext;
        private final Class<?> mActivityClass;
        private String mPhotosUri;
        private String mInitialPhotoUri;
        private String[] mProjection;
        private float mMaxInitialScale = 1.0f;
        private boolean mDisplayThumbsFullScreen;

        public PhotoViewIntentBuilder(Context context, Class<?> activityClass) {
            mContext = context;
            mActivityClass = activityClass;
        }

        public PhotoViewIntentBuilder setPhotosUri(String uri) {
            mPhotosUri = uri;
            return this;
        }

        public PhotoViewIntentBuilder setInitialPhotoUri(String uri) {
            mInitialPhotoUri = uri;
            return this;
        }

        public PhotoViewIntentBuilder setProjection(String[] projection) {
            mProjection = projection;
            return this;
        }

        public PhotoViewIntentBuilder setPhotoIndex(int index) {
            return this;
        }

        public PhotoViewIntentBuilder setResolvedPhotoUri(String uri) {
            return this;
        }

        public PhotoViewIntentBuilder setMaxInitialScale(float scale) {
            mMaxInitialScale = scale;
            return this;
        }

        public PhotoViewIntentBuilder setActionBarHiddenInitially(boolean hidden) {
            return this;
        }

        public PhotoViewIntentBuilder setDisplayThumbsFullScreen(boolean display) {
            mDisplayThumbsFullScreen = display;
            return this;
        }

        public PhotoViewIntentBuilder setScaleAnimation(int left, int top, int width, int height) {
            return this;
        }

        public Intent build() {
            final Intent intent;
            if (mActivityClass != null) {
                intent = new Intent(mContext, mActivityClass);
            } else {
                intent = new Intent();
            }
            if (mPhotosUri != null) {
                intent.putExtra(EXTRA_PHOTOS_URI, mPhotosUri);
            }
            if (mInitialPhotoUri != null) {
                intent.putExtra(EXTRA_INITIAL_PHOTO_URI, mInitialPhotoUri);
            }
            if (mProjection != null) {
                intent.putExtra(EXTRA_PROJECTION, mProjection);
            }
            intent.putExtra(EXTRA_MAX_INITIAL_SCALE, mMaxInitialScale);
            intent.putExtra(EXTRA_DISPLAY_THUMBS_FULLSCREEN, mDisplayThumbsFullScreen);
            return intent;
        }
    }
}
