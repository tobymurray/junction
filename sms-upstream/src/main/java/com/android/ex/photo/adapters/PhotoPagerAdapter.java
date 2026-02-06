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
package com.android.ex.photo.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.android.ex.photo.fragments.PhotoViewFragment;
import com.android.ex.photo.provider.PhotoContract;

public class PhotoPagerAdapter extends FragmentStatePagerAdapter {

    protected Context mContext;
    protected Cursor mCursor;
    protected float mMaxScale;
    protected boolean mThumbsFullScreen;

    public PhotoPagerAdapter(Context context, FragmentManager fm, Cursor cursor,
            float maxScale, boolean thumbsFullScreen) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mContext = context;
        mCursor = cursor;
        mMaxScale = maxScale;
        mThumbsFullScreen = thumbsFullScreen;
    }

    @Override
    public Fragment getItem(int position) {
        return createPhotoViewFragment(null, position, false);
    }

    @Override
    public int getCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    protected PhotoViewFragment createPhotoViewFragment(Intent intent, int position,
            boolean onlyShowSpinner) {
        return PhotoViewFragment.newInstance(intent, position, onlyShowSpinner);
    }

    public String getPhotoUri(Cursor cursor) {
        if (cursor == null) return null;
        final int index = cursor.getColumnIndex(PhotoContract.PhotoViewColumns.URI);
        return index >= 0 ? cursor.getString(index) : null;
    }

    public String getContentType(Cursor cursor) {
        if (cursor == null) return null;
        final int index = cursor.getColumnIndex(PhotoContract.PhotoViewColumns.CONTENT_TYPE);
        return index >= 0 ? cursor.getString(index) : null;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }
}
