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

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.loader.content.Loader;

import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.loaders.PhotoBitmapLoaderInterface.BitmapResult;

public interface PhotoViewCallbacks {

    int BITMAP_LOADER_AVATAR = PhotoViewController.BITMAP_LOADER_AVATAR;
    int BITMAP_LOADER_THUMBNAIL = PhotoViewController.BITMAP_LOADER_THUMBNAIL;
    int BITMAP_LOADER_PHOTO = PhotoViewController.BITMAP_LOADER_PHOTO;

    boolean isFragmentActive(Fragment fragment);

    PhotoPagerAdapter getAdapter();

    Loader<BitmapResult> onCreateBitmapLoader(int id, Bundle args, String uri);

    void onNewPhotoLoaded(int position);

    void toggleFullScreen();
}
