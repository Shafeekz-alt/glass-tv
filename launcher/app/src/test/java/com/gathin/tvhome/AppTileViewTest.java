package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/** Exercise the actual inflated tile, including child clipping and its animation transform. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AppTileViewTest {
    private FrameLayout scene;
    private AppTileView first, second;

    private void createScene() {
        scene = new FrameLayout(RuntimeEnvironment.getApplication());
        scene.setClipChildren(false);
        scene.setClipToPadding(false);
        for (int i = 0; i < 2; i++) {
            View item = LayoutInflater.from(scene.getContext()).inflate(R.layout.item_app, scene, false);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(220, 160);
            lp.leftMargin = 40 + i * 230;
            lp.topMargin = 70;
            scene.addView(item, lp);
            AppTileView tile = item.findViewById(R.id.card);
            tile.setFocusableInTouchMode(true);
            tile.setGlowColor(i == 0 ? 0xFFFF2020 : 0xFF0088FF);
            item.findViewById(R.id.artwork).setBackgroundColor(i == 0 ? Color.WHITE : 0xFF006AC7);
            if (i == 0) first = tile;
            else second = tile;
        }
        scene.measure(View.MeasureSpec.makeMeasureSpec(540, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(280, View.MeasureSpec.EXACTLY));
        scene.layout(0, 0, 540, 280);
    }

    private Bitmap render() {
        Bitmap bitmap = Bitmap.createBitmap(540, 280, Bitmap.Config.ARGB_8888);
        scene.draw(new Canvas(bitmap));
        return bitmap;
    }

    @Test public void borderAndHaloHugArtworkThroughoutZoom() {
        createScene();
        assertTrue(first.requestFocus());
        for (float scale : new float[]{1f, 1.035f, AppAdapter.FOCUS_SCALE, 1.09f}) {
            first.setScaleX(scale);
            first.setScaleY(scale);
            Bitmap bitmap = render();
            View item = (View) first.getParent();
            float cx = item.getLeft() + first.getLeft() + first.getWidth() / 2f;
            float cy = item.getTop() + first.getTop() + first.getHeight() / 2f;
            float right = cx + first.getWidth() * scale / 2f;
            int middle = Math.round(cy);
            // The rim must begin immediately outside the rendered white artwork, not at a
            // separately animated rectangle. A wide empty strip would fail this check.
            int rim = bitmap.getPixel((int) Math.ceil(right), middle);
            assertTrue("Missing rim beside artwork at scale " + scale, Color.alpha(rim) > 180);
            assertTrue("Rim keeps the app colour", Color.red(rim) > Color.blue(rim));
            int halo = bitmap.getPixel((int) Math.ceil(right + 7 * scale), middle);
            assertTrue("Right-side halo must extend beyond the tile at scale " + scale,
                    Color.alpha(halo) > 10);
            assertEquals("The halo cannot tint the artwork", Color.WHITE,
                    bitmap.getPixel(Math.round(cx), middle));
        }
    }

    @Test public void onlyTheCurrentlyFocusedTileHasAHalo() {
        createScene();
        assertTrue(first.requestFocus());
        int sampleX = 60 - 7;
        assertTrue(Color.alpha(render().getPixel(sampleX, 128)) > 10);
        assertTrue(second.requestFocus());
        assertEquals("No stale glow on the old app", 0, Color.alpha(render().getPixel(sampleX, 128)));
        assertTrue(Color.alpha(render().getPixel(290 - 7, 128)) > 10);
    }

    @Test public void renderActualDockTilesForReview() throws Exception {
        createScene();
        scene.setBackgroundColor(0xFF252134);
        assertTrue(first.requestFocus());
        first.setScaleX(AppAdapter.FOCUS_SCALE);
        first.setScaleY(AppAdapter.FOCUS_SCALE);
        File output = new File("build/outputs/focus-preview.png");
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            render().compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
    }
}
