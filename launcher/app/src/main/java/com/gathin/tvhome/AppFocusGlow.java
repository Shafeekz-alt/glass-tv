package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/** A bright app-coloured rim fading out into a soft halo, without a live blur or bitmap. */
final class AppFocusGlow {
    static final float FRAME_DP = 4f;
    private static final int BANDS = 10;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF band = new RectF();
    private final Matrix matrix = new Matrix();
    private final float[] hsv = new float[3];
    private LinearGradient gradient;
    private int color;

    AppFocusGlow(float density) {
        this.density = density;
        paint.setStyle(Paint.Style.STROKE);
        setColor(0xFFAAAAAA);
    }

    void setColor(int appColor) {
        if (gradient != null && color == appColor) return;
        color = appColor;
        Color.colorToHSV(appColor, hsv);
        // Dark and monochrome artwork still needs a visible selection on a bright wallpaper.
        hsv[2] = Math.max(0.85f, hsv[2]);
        int vivid = Color.HSVToColor(hsv);
        int light = Color.rgb(
                Math.round(Color.red(vivid) * 0.4f + 255 * 0.6f),
                Math.round(Color.green(vivid) * 0.4f + 255 * 0.6f),
                Math.round(Color.blue(vivid) * 0.4f + 255 * 0.6f));
        gradient = new LinearGradient(0, 0, 1, 1,
                new int[]{light, vivid, light}, new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
    }

    float reach() {
        return (BANDS * 2f + 2f) * density;
    }

    void draw(Canvas canvas, RectF bounds, float radius, float opacity) {
        if (opacity <= 0f) return;
        matrix.setScale(bounds.width(), bounds.height());
        matrix.postTranslate(bounds.left, bounds.top);
        gradient.setLocalMatrix(matrix);
        paint.setStrokeWidth(2.4f * density);
        // Concentric gradient strokes fade smoothly away from the rim. Only redrawn when the
        // shared glider moves or changes: no pulse, per-frame allocation, or GPU blur on older TVs.
        for (int i = BANDS; i >= 1; i--) {
            float out = i * 2f * density;
            float strength = 1f - (float) i / (BANDS + 1);
            paint.setAlpha(Math.round(110 * strength * strength * opacity));
            band.set(bounds);
            band.inset(-out, -out);
            canvas.drawRoundRect(band, radius + out, radius + out, paint);
        }
        // Keep the crisp rim inside the glider bounds, outside the enlarged app artwork.
        float half = 1.25f * density;
        band.set(bounds);
        band.inset(half, half);
        paint.setStrokeWidth(half * 2f);
        paint.setAlpha(Math.round(255 * opacity));
        canvas.drawRoundRect(band, Math.max(0f, radius - half), Math.max(0f, radius - half), paint);
    }
}
