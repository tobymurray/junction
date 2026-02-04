/*
 * STUB: com.android.ex.photo.adapters.PhotoPagerAdapter
 */
package com.android.ex.photo.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.android.ex.photo.fragments.PhotoViewFragment;

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
        return null;
    }

    public String getContentType(Cursor cursor) {
        return null;
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }
}
