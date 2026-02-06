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
package com.android.ex.photo.fragments;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.ex.photo.PhotoViewCallbacks;
import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.loaders.PhotoBitmapLoaderInterface.BitmapResult;
import com.android.messaging.R;

public class PhotoViewFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<BitmapResult> {

    private static final String ARG_POSITION = "position";

    // Use a unique loader ID offset per fragment position to avoid collisions
    private static final int LOADER_ID_PHOTO_BASE = 2000;

    protected PhotoViewCallbacks mCallback;
    protected Drawable mDrawable;
    private ImageView mPhotoView;
    private ProgressBar mProgressBar;
    private int mPosition;

    public static PhotoViewFragment newInstance(android.content.Intent intent, int position,
            boolean onlyShowSpinner) {
        PhotoViewFragment fragment = new PhotoViewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    protected static void initializeArguments(android.content.Intent intent, int position,
            boolean onlyShowSpinner, PhotoViewFragment fragment) {
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
    }

    public PhotoViewFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPosition = getArguments().getInt(ARG_POSITION, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_photo_view, container, false);
        mPhotoView = view.findViewById(R.id.photo_view);
        mProgressBar = view.findViewById(R.id.photo_loading);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof PhotoViewCallbacks) {
            mCallback = (PhotoViewCallbacks) getActivity();
        }
        loadPhoto();
    }

    private void loadPhoto() {
        if (mCallback == null) return;

        mProgressBar.setVisibility(View.VISIBLE);
        // Use a unique loader ID per position
        LoaderManager.getInstance(this).initLoader(
                LOADER_ID_PHOTO_BASE + mPosition, null, this);
    }

    @NonNull
    @Override
    public Loader<BitmapResult> onCreateLoader(int id, @Nullable Bundle args) {
        final String photoUri = getPhotoUri();
        return mCallback.onCreateBitmapLoader(
                PhotoViewCallbacks.BITMAP_LOADER_PHOTO, args, photoUri);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<BitmapResult> loader, BitmapResult result) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
        if (result != null && result.status == BitmapResult.STATUS_SUCCESS
                && result.drawable != null) {
            mDrawable = result.drawable;
            if (mPhotoView != null) {
                mPhotoView.setImageDrawable(mDrawable);
            }
            if (mCallback != null) {
                mCallback.onNewPhotoLoaded(mPosition);
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<BitmapResult> loader) {
        if (mPhotoView != null) {
            mPhotoView.setImageDrawable(null);
        }
        mDrawable = null;
    }

    public void onViewActivated() {
    }

    public void resetViews() {
        mDrawable = null;
        if (mPhotoView != null) {
            mPhotoView.setImageDrawable(null);
        }
    }

    protected Drawable getDrawable() {
        return mDrawable;
    }

    private String getPhotoUri() {
        if (mCallback == null) return null;
        final PhotoPagerAdapter adapter = mCallback.getAdapter();
        if (adapter == null) return null;
        final Cursor cursor = adapter.getCursor();
        if (cursor == null || mPosition >= cursor.getCount()) return null;
        cursor.moveToPosition(mPosition);
        return adapter.getPhotoUri(cursor);
    }
}
