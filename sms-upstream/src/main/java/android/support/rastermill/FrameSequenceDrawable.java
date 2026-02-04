/*
 * STUB: android.support.rastermill.FrameSequenceDrawable
 *
 * This is a stub class to allow compilation. The real implementation
 * handles animated image sequences (GIFs, WebP animations).
 *
 * TODO: Either:
 * 1. Copy the real implementation from AOSP (platform/frameworks/ex/framesequence)
 * 2. Replace with a public alternative like android-gif-drawable or Glide
 */
package android.support.rastermill;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class FrameSequenceDrawable extends Drawable {

    public interface OnFinishedListener {
        void onFinished(FrameSequenceDrawable drawable);
    }

    public FrameSequenceDrawable(FrameSequence frameSequence) {
        // Stub
    }

    public FrameSequenceDrawable(FrameSequence frameSequence, BitmapProvider provider) {
        // Stub
    }

    public interface BitmapProvider {
        Bitmap acquireBitmap(int minWidth, int minHeight);
        void releaseBitmap(Bitmap bitmap);
    }

    public void setOnFinishedListener(OnFinishedListener listener) {
        // Stub
    }

    public void start() {
        // Stub
    }

    public void stop() {
        // Stub
    }

    public void destroy() {
        // Stub - releases resources
    }

    public boolean isDestroyed() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        // Stub
    }

    @Override
    public void setAlpha(int alpha) {
        // Stub
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Stub
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 0;
    }

    @Override
    public int getIntrinsicHeight() {
        return 0;
    }
}
