/*
 * STUB: com.android.ex.photo.adapters.PhotoPagerAdapter
 */
package com.android.ex.photo.adapters;

import android.content.Context;
import android.database.Cursor;
import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public class PhotoPagerAdapter extends PagerAdapter {

    public PhotoPagerAdapter(Context context, android.app.FragmentManager fm,
            Cursor cursor, float maxScale, boolean displayFullScreen) {
    }

    public void setCursor(Cursor cursor) {
    }

    public Cursor getCursor() {
        return null;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return false;
    }
}
