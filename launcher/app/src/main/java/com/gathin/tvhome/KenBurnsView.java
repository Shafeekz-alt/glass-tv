package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/** Slow pan and zoom using the image's existing render node; no extra decoded pictures. */
final class KenBurnsView extends ImageView {
    private final ValueAnimator motion;
    private float progress;

    KenBurnsView(Context context, boolean enabled) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
        if (enabled) {
            motion = ValueAnimator.ofFloat(0f, 1f);
            motion.setDuration(12_000);
            motion.setInterpolator(new LinearInterpolator());
            motion.setRepeatCount(ValueAnimator.INFINITE);
            motion.setRepeatMode(ValueAnimator.REVERSE);
            motion.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                applyMotion();
            });
            applyMotion();
        } else {
            motion = null;
        }
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void applyMotion() {
        float zoom = 1.03f + 0.09f * progress;
        // A pivot inside the screen plus zoom >= 1 always covers all four screen edges.
        // Moving the pivot gives a gentle diagonal pan as the picture zooms in, then back out.
        setPivotX(getWidth() * (0.25f + 0.5f * progress));
        setPivotY(getHeight() * (0.75f - 0.5f * progress));
        setScaleX(zoom);
        setScaleY(zoom);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (motion != null) applyMotion();
    }

    void setMotionRunning(boolean running) {
        if (motion == null) return;
        if (!running) {
            if (motion.isStarted() && !motion.isPaused()) motion.pause();
        } else if (motion.isPaused()) {
            motion.resume();
        } else if (!motion.isStarted()) {
            motion.start();
        }
    }

    boolean isMotionRunning() {
        return motion != null && motion.isStarted() && !motion.isPaused();
    }

    void release() {
        if (motion != null) motion.cancel();
    }

    @Override protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }
}
