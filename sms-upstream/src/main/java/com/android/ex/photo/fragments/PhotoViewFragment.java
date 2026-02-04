/*
 * STUB: com.android.ex.photo.fragments.PhotoViewFragment
 */
package com.android.ex.photo.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.ex.photo.PhotoViewCallbacks;
import com.android.ex.photo.loaders.PhotoBitmapLoaderInterface.BitmapResult;

public class PhotoViewFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<BitmapResult> {

    private static final String ARG_INTENT = "intent";
    private static final String ARG_POSITION = "position";
    private static final String ARG_SHOW_SPINNER = "show_spinner";

    protected PhotoViewCallbacks mCallback;
    protected Drawable mDrawable;

    public static PhotoViewFragment newInstance(Intent intent, int position,
            boolean onlyShowSpinner) {
        PhotoViewFragment fragment = new PhotoViewFragment();
        initializeArguments(intent, position, onlyShowSpinner, fragment);
        return fragment;
    }

    protected static void initializeArguments(Intent intent, int position,
            boolean onlyShowSpinner, PhotoViewFragment fragment) {
        Bundle args = new Bundle();
        if (intent != null) {
            args.putParcelable(ARG_INTENT, intent);
        }
        args.putInt(ARG_POSITION, position);
        args.putBoolean(ARG_SHOW_SPINNER, onlyShowSpinner);
        fragment.setArguments(args);
    }

    public PhotoViewFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof PhotoViewCallbacks) {
            mCallback = (PhotoViewCallbacks) getActivity();
        }
    }

    @Override
    public Loader<BitmapResult> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<BitmapResult> loader, BitmapResult result) {
        if (result != null && result.drawable != null) {
            mDrawable = result.drawable;
        }
    }

    @Override
    public void onLoaderReset(Loader<BitmapResult> loader) {
        mDrawable = null;
    }

    public void onViewActivated() {
        // Called when fragment becomes visible
    }

    public void resetViews() {
        // Reset views
        mDrawable = null;
    }

    protected Drawable getDrawable() {
        return mDrawable;
    }
}
