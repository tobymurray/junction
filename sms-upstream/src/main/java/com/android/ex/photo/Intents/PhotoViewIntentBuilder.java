/*
 * STUB: com.android.ex.photo.Intents.PhotoViewIntentBuilder
 *
 * This is a stub class to allow compilation.
 */
package com.android.ex.photo.Intents;

import android.content.Context;
import android.content.Intent;

public class PhotoViewIntentBuilder {

    private Context mContext;
    private Class<?> mActivityClass;

    public PhotoViewIntentBuilder(Context context, Class<?> activityClass) {
        mContext = context;
        mActivityClass = activityClass;
    }

    public PhotoViewIntentBuilder setPhotosUri(String uri) {
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

    public Intent build() {
        if (mActivityClass != null) {
            return new Intent(mContext, mActivityClass);
        }
        return new Intent();
    }
}
