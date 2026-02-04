/*
 * STUB: com.android.ex.photo.Intents
 *
 * This is a stub class to allow compilation.
 */
package com.android.ex.photo;

import android.content.Context;

public class Intents {

    /**
     * Creates a new PhotoViewIntentBuilder for building an intent to launch the photo viewer.
     */
    public static PhotoViewIntentBuilder newPhotoViewIntentBuilder(
            Context context, Class<?> activityClass) {
        return new PhotoViewIntentBuilder(context, activityClass);
    }

    /**
     * Builder class for creating photo view intents.
     */
    public static class PhotoViewIntentBuilder {

        private Context mContext;
        private Class<?> mActivityClass;

        public PhotoViewIntentBuilder(Context context, Class<?> activityClass) {
            mContext = context;
            mActivityClass = activityClass;
        }

        public PhotoViewIntentBuilder setPhotosUri(String uri) {
            return this;
        }

        public PhotoViewIntentBuilder setInitialPhotoUri(String uri) {
            return this;
        }

        public PhotoViewIntentBuilder setProjection(String[] projection) {
            return this;
        }

        public PhotoViewIntentBuilder setPhotoIndex(int index) {
            return this;
        }

        public PhotoViewIntentBuilder setResolvedPhotoUri(String uri) {
            return this;
        }

        public PhotoViewIntentBuilder setMaxInitialScale(float scale) {
            return this;
        }

        public PhotoViewIntentBuilder setActionBarHiddenInitially(boolean hidden) {
            return this;
        }

        public PhotoViewIntentBuilder setDisplayThumbsFullScreen(boolean display) {
            return this;
        }

        public PhotoViewIntentBuilder setScaleAnimation(int left, int top, int width, int height) {
            return this;
        }

        public android.content.Intent build() {
            if (mActivityClass != null) {
                return new android.content.Intent(mContext, mActivityClass);
            }
            return new android.content.Intent();
        }
    }
}
