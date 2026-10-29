package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/** Artwork and focus share one transform, so the border cannot drift during zoom or scrolling. */
public final class AppTileView extends FrameLayout {
    private final AppFocusGlow glow;
    private final RectF focusBounds = new RectF();
    private final Path artworkClip = new Path();
    private final float density;

    public AppTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        glow = new AppFocusGlow(density);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float radius = AppAdapter.cornerRadius(w, density);
        artworkClip.rewind();
        artworkClip.addRoundRect(0, 0, w, h, radius, radius, Path.Direction.CW);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        // Clip just the artwork children, after onDraw has painted the halo outside the tile.
        int saved = canvas.save();
        canvas.clipPath(artworkClip);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saved);
    }

    void setGlowColor(int color) {
        glow.setColor(color);
        invalidate();
    }

    @Override protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isFocused() || getWidth() == 0 || getHeight() == 0) return;
        float frame = AppFocusGlow.FRAME_DP * density;
        float radius = AppAdapter.cornerRadius(getWidth(), density) + frame;
        focusBounds.set(-frame, -frame, getWidth() + frame, getHeight() + frame);
        glow.drawHalo(canvas, focusBounds, radius, 1f);
        glow.drawRim(canvas, focusBounds, radius, 1f);
    }
}
