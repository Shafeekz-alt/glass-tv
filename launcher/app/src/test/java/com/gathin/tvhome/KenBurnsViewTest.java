package com.gathin.tvhome;

import static org.junit.Assert.*;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class KenBurnsViewTest {
    private KenBurnsView view(boolean enabled) {
        KenBurnsView view = new KenBurnsView(RuntimeEnvironment.getApplication(), enabled);
        view.layout(0, 0, 1920, 1080);
        return view;
    }

    @Test public void motionAlwaysCoversTheScreenAndMovesThePicture() {
        KenBurnsView view = view(true);
        view.setMotionRunning(true);
        try {
            float initialZoom = view.getScaleX();
            float initialPivot = view.getPivotX();
            ValueAnimator animator = ReflectionHelpers.getField(view, "motion");
            // Seek bounded frames in both directions. Robolectric caps INFINITE at one repeat;
            // forcing an endless real animator makes its virtual frame scheduler fail to drain.
            assertEquals(ValueAnimator.INFINITE,
                    org.robolectric.Shadows.shadowOf(animator).getActualRepeatCount());
            for (int i = 0; i < 40; i++) {
                animator.setCurrentPlayTime(i * 500L);
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
            ValueAnimator animator = ReflectionHelpers.getField(view, "motion");
            animator.setCurrentPlayTime(3_000);
            view.setMotionRunning(false);
            float paused = view.getScaleX();
            assertFalse(view.isMotionRunning());
            assertTrue(animator.isPaused());
            assertEquals(paused, view.getScaleX(), 0.0001f);
            view.setMotionRunning(true);
            assertEquals("Resuming must not jump back", paused, view.getScaleX(), 0.0001f);
            assertTrue("Animation after resume: started=" + animator.isStarted()
                    + " paused=" + animator.isPaused(), view.isMotionRunning());
            animator.setCurrentPlayTime(5_000);
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
            ValueAnimator animator = ReflectionHelpers.getField(view, "motion");
            animator.setCurrentPlayTime(5_000);
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
        assertFalse(view.isMotionRunning());
        assertEquals(1f, view.getScaleX(), 0f);
        assertEquals(1f, view.getScaleY(), 0f);
    }
}
