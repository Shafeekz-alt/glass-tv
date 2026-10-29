package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/** A single app-coloured rim with a continuous halo, without a live blur or bitmap. */
final class AppFocusGlow {
    static final float FRAME_DP = 3f;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Adjacent rectangular patches share exact edges. Antialiasing their boundaries would leave
    // hairline seams; the shaders themselves provide the soft, rounded outer boundary.
    private final Paint haloPaint = new Paint(Paint.DITHER_FLAG);
    private final RectF band = new RectF();
    private final Matrix matrix = new Matrix();
    private final float[] hsv = new float[3];
    private LinearGradient gradient;
    private LinearGradient edge;
    private RadialGradient corner;
    private final int[] haloColors = new int[5];
    private static final float[] FADE = {0f, 0.18f, 0.45f, 0.75f, 1f};
    private float cornerRadius = -1f;
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
                Math.round(Color.red(vivid) * 0.65f + 255 * 0.35f),
                Math.round(Color.green(vivid) * 0.65f + 255 * 0.35f),
                Math.round(Color.blue(vivid) * 0.65f + 255 * 0.35f));
        gradient = new LinearGradient(0, 0, 1, 1,
                new int[]{light, vivid, light}, new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        int rgb = vivid & 0x00FFFFFF;
        haloColors[0] = 0x90000000 | rgb;
        haloColors[1] = 0x70000000 | rgb;
        haloColors[2] = 0x36000000 | rgb;
        haloColors[3] = 0x0C000000 | rgb;
        haloColors[4] = rgb; // Transparent app colour, avoiding a dark fringe.
        edge = new LinearGradient(0, 0, 0, 1, haloColors, FADE, Shader.TileMode.CLAMP);
        corner = null;
    }

    float reach() {
        return 20f * density;
    }

    /** Paint before the glass fill: only the halo outside it should be visible. */
    void drawHalo(Canvas canvas, RectF bounds, float radius, float opacity) {
        if (opacity <= 0f) return;
        float spread = reach();
        radius = Math.max(0f, Math.min(radius, Math.min(bounds.width(), bounds.height()) / 2f));
        if (corner == null || cornerRadius != radius) {
            float[] stops = new float[FADE.length];
            for (int i = 0; i < stops.length; i++) {
                stops[i] = (radius + spread * FADE[i]) / (radius + spread);
            }
            corner = new RadialGradient(0, 0, 1, haloColors, stops, Shader.TileMode.CLAMP);
            cornerRadius = radius;
        }
        haloPaint.setAlpha(Math.round(255 * opacity));
        float l = bounds.left, t = bounds.top, r = bounds.right, b = bounds.bottom;
        haloPaint.setShader(edge);
        matrix.setScale(1, -spread);
        matrix.postTranslate(0, t);
        edge.setLocalMatrix(matrix);
        canvas.drawRect(l + radius, t - spread, r - radius, t, haloPaint);
        matrix.setScale(1, spread);
        matrix.postTranslate(0, b);
        edge.setLocalMatrix(matrix);
        canvas.drawRect(l + radius, b, r - radius, b + spread, haloPaint);
        matrix.setScale(1, spread);
        matrix.postRotate(90);
        matrix.postTranslate(l, 0);
        edge.setLocalMatrix(matrix);
        canvas.drawRect(l - spread, t + radius, l, b - radius, haloPaint);
        matrix.setScale(1, spread);
        matrix.postRotate(-90);
        matrix.postTranslate(r, 0);
        edge.setLocalMatrix(matrix);
        canvas.drawRect(r, t + radius, r + spread, b - radius, haloPaint);
        haloPaint.setShader(corner);
        float outer = radius + spread;
        drawCorner(canvas, l - spread, t - spread, l + radius, t + radius, l + radius, t + radius, outer);
        drawCorner(canvas, r - radius, t - spread, r + spread, t + radius, r - radius, t + radius, outer);
        drawCorner(canvas, l - spread, b - radius, l + radius, b + spread, l + radius, b - radius, outer);
        drawCorner(canvas, r - radius, b - radius, r + spread, b + spread, r - radius, b - radius, outer);
    }

    private void drawCorner(Canvas canvas, float l, float t, float r, float b, float cx, float cy, float outer) {
        matrix.setScale(outer, outer);
        matrix.postTranslate(cx, cy);
        corner.setLocalMatrix(matrix);
        canvas.drawRect(l, t, r, b, haloPaint);
    }

    void drawRim(Canvas canvas, RectF bounds, float radius, float opacity) {
        if (opacity <= 0f) return;
        matrix.setScale(bounds.width(), bounds.height());
        matrix.postTranslate(bounds.left, bounds.top);
        gradient.setLocalMatrix(matrix);
        // Keep the crisp rim inside the glider bounds, outside the enlarged app artwork.
        float half = density;
        band.set(bounds);
        band.inset(half, half);
        paint.setStrokeWidth(half * 2f);
        paint.setAlpha(Math.round(255 * opacity));
        canvas.drawRoundRect(band, Math.max(0f, radius - half), Math.max(0f, radius - half), paint);
    }
}
