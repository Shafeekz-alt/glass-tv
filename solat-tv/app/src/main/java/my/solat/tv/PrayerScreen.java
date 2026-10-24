package my.solat.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A quiet, overscan-safe television face. All geometry uses a 1280 by 720 canvas. */
public final class PrayerScreen extends View {
    private static final Locale MS = new Locale("ms", "MY");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", MS);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", MS);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", MS);
    private static final int BACKGROUND = Color.rgb(5, 17, 20);
    private static final int WHITE = Color.rgb(207, 220, 215);
    private static final int MUTED = Color.rgb(115, 143, 140);
    private static final int GOLD = Color.rgb(190, 162, 102);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Bitmap photo;
    private Bitmap previousPhoto;
    private String photoCaption = "";
    private long photoChangedAt;
    private boolean attached;
    private static final long PHOTO_FADE_MS = 2000;
    private PrayerSchedule.Day day;
    private PrayerSchedule.Event next;
    private Instant now = Instant.now();
    private String status = "Memuatkan waktu solat…";
    private boolean soundEnabled;

    public PrayerScreen(Context context) {
        super(context);
        setFocusable(false);
        setBackgroundColor(BACKGROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void show(PrayerSchedule.Day day, PrayerSchedule.Event next, Instant now,
                     String status, boolean soundEnabled) {
        this.day = day;
        this.next = next;
        this.now = now == null ? Instant.now() : now;
        this.status = status == null ? "" : status;
        this.soundEnabled = soundEnabled;
        setContentDescription("Waktu solat Kuala Terengganu. " +
                (next == null ? "Waktu seterusnya belum tersedia." : "Seterusnya " + next.label) +
                ". " + this.status);
        invalidate();
    }

    public void setBackgroundPhoto(Bitmap image, String caption) {
        if (image != photo) {
            previousPhoto = photo;
            photo = image;
            photoChangedAt = android.os.SystemClock.uptimeMillis();
        }
        photoCaption = image == null || caption == null ? "" : caption;
        if (image == null) previousPhoto = null;
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        previousPhoto = null;
        super.onDetachedFromWindow();
    }

    private void drawPhoto(Canvas canvas, Bitmap bitmap, int alpha) {
        if (bitmap == null || bitmap.isRecycled()) return;
        float scale = Math.max(getWidth() / (float) bitmap.getWidth(),
                getHeight() / (float) bitmap.getHeight());
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        rect.set((getWidth() - width) / 2, (getHeight() - height) / 2,
                (getWidth() + width) / 2, (getHeight() + height) / 2);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(alpha);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, null, rect, paint);
        paint.setAlpha(255);
    }

    private void text(Canvas c, String value, float x, float y, float size,
                      int color, boolean bold, Paint.Align align) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(size);
        paint.setTypeface(bold ? Typeface.create("sans-serif-medium", Typeface.NORMAL)
                : Typeface.create("sans-serif-light", Typeface.NORMAL));
        paint.setTextAlign(align);
        c.drawText(value, x, y, paint);
    }

    private void fitted(Canvas c, String value, float x, float y, float size,
                        float width, int color, Paint.Align align) {
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(size);
        float measured = paint.measureText(value);
        text(c, value, x, y, measured > width ? size * width / measured : size,
                color, false, align);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (photo != null && !photo.isRecycled()) {
            long elapsed = android.os.SystemClock.uptimeMillis() - photoChangedAt;
            float progress = Math.min(1f, Math.max(0f, elapsed / (float) PHOTO_FADE_MS));
            drawPhoto(canvas, previousPhoto, 255);
            drawPhoto(canvas, photo, Math.round(progress * 255));
            canvas.drawColor(Color.argb(195, 3, 14, 17));
            if (progress < 1f && attached) postInvalidateOnAnimation();
            else if (progress >= 1f) previousPhoto = null;
        }
        float scale = Math.min(getWidth() / 1280f, getHeight() / 720f);
        if (scale <= 0) return;
        canvas.save();
        canvas.translate((getWidth() - 1280 * scale) / 2f,
                (getHeight() - 720 * scale) / 2f);
        canvas.scale(scale, scale);
        // Slow, continuous movement stays well inside the safe television margins.
        double phase = now.toEpochMilli() / 60000.0;
        canvas.translate((float) (Math.sin(phase / 9.0) * 7),
                (float) (Math.cos(phase / 13.0) * 5));
        ZonedDateTime local = now.atZone(PrayerSchedule.ZONE);
        LocalDate today = local.toLocalDate();
        boolean validDay = day != null && day.date.equals(today);
        text(canvas, "W A K T U   S O L A T", 76, 83, 15, GOLD, true, Paint.Align.LEFT);
        text(canvas, "Kuala Terengganu", 76, 121, 29, WHITE, false, Paint.Align.LEFT);
        text(canvas, "TRG01  ·  MALAYSIA", 1204, 91, 14, MUTED, true, Paint.Align.RIGHT);
        paint.setColor(Color.rgb(31, 52, 53));
        paint.setStrokeWidth(1);
        canvas.drawLine(76, 146, 1204, 146, paint);

        text(canvas, CLOCK.format(local), 72, 315, 151, WHITE, false, Paint.Align.LEFT);
        text(canvas, DATE.format(local), 80, 357, 23, MUTED, false, Paint.Align.LEFT);
        if (validDay && !day.hijri.isEmpty()) {
            fitted(canvas, day.hijri, 80, 388, 17, 570, MUTED, Paint.Align.LEFT);
        }
        paint.setColor(Color.rgb(35, 57, 56));
        canvas.drawLine(758, 204, 758, 389, paint);
        text(canvas, "SOLAT SETERUSNYA", 807, 224, 14, GOLD, true, Paint.Align.LEFT);
        if (next == null) {
            text(canvas, "—", 803, 282, 44, WHITE, false, Paint.Align.LEFT);
            text(canvas, "Waktu belum tersedia", 807, 326, 20, MUTED, false, Paint.Align.LEFT);
        } else {
            boolean tomorrow = next.at.atZone(PrayerSchedule.ZONE).toLocalDate().isAfter(today);
            text(canvas, next.label + (tomorrow ? " · Esok" : ""), 803, 280,
                    38, WHITE, false, Paint.Align.LEFT);
            long seconds = Math.max(0, Duration.between(now, next.at).getSeconds());
            String countdown = String.format(MS, "%02d:%02d:%02d", seconds / 3600,
                    (seconds % 3600) / 60, seconds % 60);
            text(canvas, countdown, 803, 332, 38, GOLD, false, Paint.Align.LEFT);
            text(canvas, "pada " + TIME.format(next.at.atZone(PrayerSchedule.ZONE)),
                    807, 365, 18, MUTED, false, Paint.Align.LEFT);
        }

        float gap = 14;
        float width = (1128 - gap * 5) / 6;
        for (int i = 0; i < PrayerSchedule.KEYS.length; i++) {
            float x = 76 + i * (width + gap);
            boolean selected = validDay && next != null && next.key.equals(PrayerSchedule.KEYS[i])
                    && next.at.atZone(PrayerSchedule.ZONE).toLocalDate().equals(today);
            rect.set(x, 447, x + width, 566);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(selected ? Color.rgb(24, 44, 43) : Color.rgb(10, 26, 29));
            canvas.drawRoundRect(rect, 8, 8, paint);
            if (selected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);
                paint.setColor(Color.rgb(106, 96, 66));
                canvas.drawRoundRect(rect, 8, 8, paint);
            }
            text(canvas, PrayerSchedule.LABELS[i], x + 18, 482, 18,
                    selected ? GOLD : MUTED, true, Paint.Align.LEFT);
            String time = validDay && day.times.get(PrayerSchedule.KEYS[i]) != null
                    ? TIME.format(day.times.get(PrayerSchedule.KEYS[i])) : "—";
            text(canvas, time, x + 17, 535, 34, selected ? WHITE : MUTED,
                    false, Paint.Align.LEFT);
        }
        fitted(canvas, status, 76, 614, 15, 870, MUTED, Paint.Align.LEFT);
        text(canvas, soundEnabled ? "AZAN  ·  AKTIF" : "AZAN  ·  SENYAP", 1204, 614,
                13, soundEnabled ? GOLD : MUTED, true, Paint.Align.RIGHT);
        text(canvas, "Sumber waktu solat: JAKIM / e-Solat", 76, 651, 13,
                MUTED, false, Paint.Align.LEFT);
        text(canvas, "Waktu Malaysia  ·  UTC+08:00", 1204, 651, 13,
                MUTED, false, Paint.Align.RIGHT);
        if (!photoCaption.isEmpty()) {
            fitted(canvas, photoCaption, 640, 683, 12, 1000, MUTED, Paint.Align.CENTER);
        }
        canvas.restore();
    }
}
