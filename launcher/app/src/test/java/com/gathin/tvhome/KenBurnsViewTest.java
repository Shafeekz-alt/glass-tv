package com.gathin.tvhome;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;

import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class KenBurnsViewTest {
    private KenBurnsView view(boolean enabled) {
        KenBurnsView view = new KenBurnsView(RuntimeEnvironment.getApplication(), enabled);
        // ShadowValueAnimator rewrites INFINITE to one repeat. Restore Android's real repeat
        // count so pause/resume and coverage checks can exercise an actual continuous loop.
        ValueAnimator animator = ReflectionHelpers.getField(view, "motion");
        if (animator != null) ReflectionHelpers.setField(animator, "mRepeatCount", ValueAnimator.INFINITE);
        view.layout(0, 0, 1920, 1080);
        return view;
    }

    @Test public void motionAlwaysCoversTheScreenAndMovesThePicture() {
        KenBurnsView view = view(true);
        view.setMotionRunning(true);
        try {
            float initialZoom = view.getScaleX();
            float initialPivot = view.getPivotX();
            for (int i = 0; i < 30; i++) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
                RectF covered = new RectF(0, 0, 1920, 1080);
                view.getMatrix().mapRect(covered);
                assertTrue("No exposed left/top edges", covered.left <= 0 && covered.top <= 0);
                assertTrue("No exposed right/bottom edges", covered.right >= 1920 && covered.bottom >= 1080);
            }
            assertNotEquals(initialZoom, view.getScaleX(), 0.001f);
            assertNotEquals(initialPivot, view.getPivotX(), 1f);
        } finally {
            view.release();
        }
    }

    @Test public void pauseFreezesMotionAndResumeContinuesWithoutReset() {
        KenBurnsView view = view(true);
        view.setMotionRunning(true);
        try {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
            view.setMotionRunning(false);
            float paused = view.getScaleX();
            assertFalse(view.isMotionRunning());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));
            assertEquals(paused, view.getScaleX(), 0.0001f);
            view.setMotionRunning(true);
            assertEquals("Resuming must not jump back", paused, view.getScaleX(), 0.0001f);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
            ValueAnimator animator = ReflectionHelpers.getField(view, "motion");
            assertTrue("Animation after resume: started=" + animator.isStarted()
                    + " paused=" + animator.isPaused(), view.isMotionRunning());
            assertNotEquals(paused, view.getScaleX(), 0.001f);
        } finally {
            view.release();
        }
        assertFalse(view.isMotionRunning());
    }

    @Test public void changingPicturesDoesNotResetThePanOrZoom() {
        KenBurnsView view = view(true);
        view.setMotionRunning(true);
        try {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
            float scale = view.getScaleX(), pivot = view.getPivotX();
            view.setImageBitmap(Bitmap.createBitmap(32, 18, Bitmap.Config.RGB_565));
            assertEquals(scale, view.getScaleX(), 0f);
            assertEquals(pivot, view.getPivotX(), 0f);
        } finally {
            view.release();
        }
    }

    @Test public void disabledMotionLeavesTheWallpaperStill() {
        KenBurnsView view = view(false);
        view.setMotionRunning(true);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(15));
        assertFalse(view.isMotionRunning());
        assertEquals(1f, view.getScaleX(), 0f);
        assertEquals(1f, view.getScaleY(), 0f);
    }
}
