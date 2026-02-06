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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.loaders.PhotoBitmapLoaderInterface.BitmapResult;
import com.android.ex.photo.provider.PhotoContract;
import com.android.messaging.R;

public class PhotoViewActivity extends AppCompatActivity
        implements PhotoViewCallbacks, PhotoViewController.ActivityInterface,
        LoaderManager.LoaderCallbacks<Cursor>, ViewPager.OnPageChangeListener {

    private static final int LOADER_PHOTO_LIST = 100;

    protected PhotoViewController mController;
    protected ViewPager mViewPager;
    protected PhotoPagerAdapter mAdapter;

    private String mPhotosUri;
    private String mInitialPhotoUri;
    private String[] mProjection;
    private float mMaxInitialScale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        mController = createController();
        mViewPager = findViewById(R.id.photo_view_pager);
        mViewPager.addOnPageChangeListener(this);

        final Intent intent = getIntent();
        mPhotosUri = intent.getStringExtra(Intents.EXTRA_PHOTOS_URI);
        mInitialPhotoUri = intent.getStringExtra(Intents.EXTRA_INITIAL_PHOTO_URI);
        mProjection = intent.getStringArrayExtra(Intents.EXTRA_PROJECTION);
        mMaxInitialScale = intent.getFloatExtra(Intents.EXTRA_MAX_INITIAL_SCALE, 1.0f);
        mController.mDisplayThumbsFullScreen = intent.getBooleanExtra(
                Intents.EXTRA_DISPLAY_THUMBS_FULLSCREEN, false);

        LoaderManager.getInstance(this).initLoader(LOADER_PHOTO_LIST, null, this);
    }

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
            }
        };
    }

    // --- PhotoViewCallbacks ---

    @Override
    public boolean isFragmentActive(Fragment fragment) {
        return fragment != null && fragment.isResumed();
    }

    @Override
    public PhotoPagerAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public Loader<BitmapResult> onCreateBitmapLoader(int id, Bundle args, String uri) {
        return mController.onCreateBitmapLoader(id, args, uri);
    }

    @Override
    public void onNewPhotoLoaded(int position) {
        mController.updateActionBar();
    }

    @Override
    public void toggleFullScreen() {
    }

    // --- LoaderCallbacks<Cursor> ---

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mPhotosUri != null) {
            return new CursorLoader(this, Uri.parse(mPhotosUri), mProjection, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            mController.mIsEmpty = true;
            return;
        }

        mAdapter = mController.createPhotoPagerAdapter(
                this, getSupportFragmentManager(), cursor, mMaxInitialScale);
        mViewPager.setAdapter(mAdapter);

        // Navigate to the initial photo
        if (mInitialPhotoUri != null) {
            int position = findPhotoPosition(cursor);
            if (position >= 0) {
                mViewPager.setCurrentItem(position, false);
            }
        }

        mController.updateActionBar();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null) {
            mAdapter.swapCursor(null);
            mAdapter = null;
        }
    }

    // --- ViewPager.OnPageChangeListener ---

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        mController.updateActionBar();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    // --- Helpers ---

    private int findPhotoPosition(Cursor cursor) {
        final int uriColumn = cursor.getColumnIndex(PhotoContract.PhotoViewColumns.URI);
        if (uriColumn < 0) return 0;

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            final String uri = cursor.getString(uriColumn);
            if (mInitialPhotoUri.equals(uri)) {
                return cursor.getPosition();
            }
        }
        return 0;
    }
}
