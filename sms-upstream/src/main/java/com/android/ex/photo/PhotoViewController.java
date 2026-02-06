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
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.fragment.app.FragmentManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

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
    public boolean mIsEmpty;
    public boolean mDisplayThumbsFullScreen;

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
        if (!(mActivity instanceof PhotoViewActivity)) {
            return null;
        }
        final PhotoViewActivity activity = (PhotoViewActivity) mActivity;
        final ViewPager pager = activity.mViewPager;
        final PhotoPagerAdapter adapter = activity.mAdapter;
        if (pager == null || adapter == null || adapter.getCursor() == null) {
            return null;
        }
        final int position = pager.getCurrentItem();
        final Cursor cursor = adapter.getCursor();
        if (cursor.getCount() == 0 || position >= cursor.getCount()) {
            return null;
        }
        cursor.moveToPosition(position);
        return cursor;
    }

    protected PhotoPagerAdapter getAdapter() {
        if (mActivity instanceof PhotoViewActivity) {
            return ((PhotoViewActivity) mActivity).mAdapter;
        }
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
