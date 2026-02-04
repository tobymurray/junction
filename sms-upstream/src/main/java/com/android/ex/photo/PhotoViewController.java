/*
 * STUB: com.android.ex.photo.PhotoViewController
 */
package com.android.ex.photo;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.fragment.app.FragmentManager;
import androidx.loader.content.Loader;

import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.loaders.PhotoBitmapLoaderInterface.BitmapResult;

public class PhotoViewController {

    public static final int BITMAP_LOADER_AVATAR = 1;
    public static final int BITMAP_LOADER_THUMBNAIL = 2;
    public static final int BITMAP_LOADER_PHOTO = 3;

    public static int sMaxPhotoSize = 2048;

    protected ActivityInterface mActivity;
    protected String mActionBarTitle;
    protected String mActionBarSubtitle;
    protected boolean mIsEmpty;
    protected boolean mDisplayThumbsFullScreen;

    public interface ActivityInterface {
        Context getContext();
        ActionBarInterface getActionBarInterface();
    }

    public interface ActionBarInterface {
        void setTitle(CharSequence title);
        void setSubtitle(CharSequence subtitle);
    }

    public PhotoViewController(ActivityInterface activity) {
        mActivity = activity;
    }

    public ActivityInterface getActivity() {
        return mActivity;
    }

    public Loader<BitmapResult> onCreateBitmapLoader(int id, Bundle args, String uri) {
        return null;
    }

    public void updateActionBar() {
    }

    protected Cursor getCursorAtProperPosition() {
        return null;
    }

    protected PhotoPagerAdapter getAdapter() {
        return null;
    }

    protected void setActionBarTitles(ActionBarInterface actionBar) {
        if (actionBar != null) {
            actionBar.setTitle(mActionBarTitle);
            actionBar.setSubtitle(mActionBarSubtitle);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public PhotoPagerAdapter createPhotoPagerAdapter(Context context, FragmentManager fm,
            Cursor c, float maxScale) {
        return new PhotoPagerAdapter(context, fm, c, maxScale, mDisplayThumbsFullScreen);
    }
}
