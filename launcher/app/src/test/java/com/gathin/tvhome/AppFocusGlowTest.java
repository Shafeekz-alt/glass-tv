package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/** Pixel checks on Android's native renderer: stepped strokes must not return as halo rings. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AppFocusGlowTest {
    private static final RectF TILE = new RectF(48, 48, 288, 183);
    private static final float RADIUS = 24f;

    private Bitmap halo(float opacity) {
        Bitmap bitmap = Bitmap.createBitmap(336, 231, Bitmap.Config.ARGB_8888);
        AppFocusGlow glow = new AppFocusGlow(1f);
        glow.setColor(0xFF087BDF);
        glow.drawHalo(new Canvas(bitmap), TILE, RADIUS, opacity);
        return bitmap;
    }

    @Test public void haloFadesContinuouslyWithoutConcentricRings() {
        Bitmap bitmap = halo(1f);
        int previous = 255;
        int visible = 0;
        for (int distance = 1; distance <= 22; distance++) {
            int alpha = Color.alpha(bitmap.getPixel(168, 48 - distance));
            assertTrue("Halo brightens again at distance " + distance, alpha <= previous + 1);
            if (alpha > 0) visible++;
            previous = alpha;
        }
        assertTrue("Halo must fade over a broad area", visible >= 16);
        assertEquals(0, previous);
    }

    @Test public void roundedCornersJoinEdgesWithoutSeamsOrSquareSpill() {
        Bitmap bitmap = halo(1f);
        int edgeAlpha = Color.alpha(bitmap.getPixel(100, 40));
        for (int x = 70; x <= 74; x++) {
            assertEquals("Corner/edge seam at x=" + x, edgeAlpha,
                    Color.alpha(bitmap.getPixel(x, 40)), 2);
        }
        assertEquals("Outer corner must stay transparent", 0,
                Color.alpha(bitmap.getPixel(28, 28)));
        int previous = 255;
        // Start outside the curved border; its inside is deliberately transparent now.
        for (int step = 0; step <= 18; step++) {
            int alpha = Color.alpha(bitmap.getPixel(54 - step, 54 - step));
            assertTrue("Corner fade must not form rings", alpha <= previous + 1);
            previous = alpha;
        }
        assertEquals(0, previous);
    }

    @Test public void haloRespectsFocusOpacity() {
        int full = Color.alpha(halo(1f).getPixel(168, 44));
        int half = Color.alpha(halo(0.5f).getPixel(168, 44));
        assertEquals(full / 2f, half, 1f);
        assertEquals(0, Color.alpha(halo(0f).getPixel(168, 44)));
    }

    @Test public void haloNeverFillsTheInsideOfItsRoundedBorder() {
        Bitmap bitmap = halo(1f);
        assertEquals("Straight interior stays empty", 0, Color.alpha(bitmap.getPixel(285, 115)));
        assertEquals("Corner interior stays empty too", 0, Color.alpha(bitmap.getPixel(278, 58)));
        assertEquals("Centre stays empty", 0, Color.alpha(bitmap.getPixel(168, 115)));
    }

    @Test public void renderSingleRoundedRimAndPreview() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(336, 231, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        AppFocusGlow glow = new AppFocusGlow(1f);
        glow.setColor(0xFF087BDF);
        glow.drawHalo(canvas, TILE, RADIUS, 1f);
        glow.drawRim(canvas, TILE, RADIUS, 1f);
        assertTrue("Top edge must remain clear", Color.alpha(bitmap.getPixel(168, 49)) > 230);
        assertTrue("Rim must round away from the corner", Color.alpha(bitmap.getPixel(49, 49)) < 150);

        // A reviewable image made by the same Android renderer as the pixel tests.
        bitmap.eraseColor(0xFF202530);
        glow.drawHalo(canvas, TILE, RADIUS, 1f);
        Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
        tile.setColor(0xFF075BA4);
        canvas.drawRoundRect(51, 51, 285, 180, 21, 21, tile);
        glow.drawRim(canvas, TILE, RADIUS, 1f);
        tile.setColor(Color.WHITE);
        tile.setTextSize(22);
        tile.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Selected app", 168, 124, tile);
        File output = new File("build/outputs/glow-only-preview.png");
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
    }
}
