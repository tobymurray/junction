/*
 * STUB: com.android.ex.photo.PhotoViewActivity
 *
 * This is a stub class to allow compilation.
 */
package com.android.ex.photo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public class PhotoViewActivity extends Activity implements PhotoViewController.ActivityInterface {

    protected PhotoViewController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mController = createController();
    }

    /**
     * Create the PhotoViewController for this activity.
     * Subclasses should override this to provide their own controller.
     */
    public PhotoViewController createController() {
        return new PhotoViewController(this);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public PhotoViewController.ActionBarInterface getActionBarInterface() {
        return new PhotoViewController.ActionBarInterface() {
            @Override
            public void setTitle(CharSequence title) {
                PhotoViewActivity.this.setTitle(title);
            }

            @Override
            public void setSubtitle(CharSequence subtitle) {
                // Activity doesn't have subtitle by default
            }
        };
    }
}
