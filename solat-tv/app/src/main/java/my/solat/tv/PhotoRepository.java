package my.solat.tv;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Fixed licensed collection. Downloads/decodes run away from the prayer clock. */
final class PhotoRepository {
    private final Context context;
    private final PrayerScreen screen;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean stopped;
    private int cursor;
    PhotoRepository(Context context, PrayerScreen screen) { this.context = context; this.screen = screen; }
    void start() { handler.post(rotate); }
    private final Runnable rotate = new Runnable() { @Override public void run() {
        if (stopped) return;
        worker.execute(() -> {
            try {
                JSONArray items = catalog(context);
                for (int tries = 0; tries < items.length() && !stopped; tries++) {
                    int index = cursor++ % items.length();
                    JSONObject item = items.getJSONObject(index);
                    try {
                        Bitmap image = image(index, item.getString("url"));
                        String caption = item.getString("name");
                        if (image != null) {
                            handler.post(() -> { if (!stopped) screen.setBackgroundPhoto(image, caption); });
                            break;
                        }
                    } catch (Exception ignored) { /* Retain last good background; prayer data is independent. */ }
                }
            } catch (Exception ignored) {}
            handler.post(() -> { if (!stopped) handler.postDelayed(PhotoRepository.this.rotate, 120_000); });
        });
    }};
    private Bitmap image(int index, String source) throws IOException {
        File folder = new File(context.getFilesDir(), "mosques");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Cannot create image cache");
        File file = new File(folder, "mosque-" + index + ".jpg");
        if (!file.isFile()) {
            URL url = new URL(source);
            if (!"https".equals(url.getProtocol()) || !("upload.wikimedia.org".equals(url.getHost()) || "thumb.wikimedia.org".equals(url.getHost()))) throw new IOException("Unexpected image source");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(12_000); c.setReadTimeout(12_000); c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "WaktuSolatTV/1.0 (personal Android TV screensaver)");
            File temp = new File(folder, "mosque-" + index + ".part-" + Thread.currentThread().getId());
            try {
                if (c.getResponseCode() != 200) throw new IOException("Image HTTP " + c.getResponseCode());
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[8192]; int n, total = 0;
                    while (!stopped && (n = in.read(buffer)) != -1) {
                        total += n;
                        if (total > 12 * 1024 * 1024) throw new IOException("Image too large");
                        out.write(buffer, 0, n);
                    }
                }
                if (stopped) return null;
                BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(temp.getPath(), bounds);
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Invalid photo");
                if (!temp.renameTo(file)) throw new IOException("Cannot save photo");
            } finally { c.disconnect(); if (temp.exists()) temp.delete(); }
        }
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) { file.delete(); throw new IOException("Corrupt image cache"); }
        options.inSampleSize = 1;
        while (options.outWidth / options.inSampleSize > 1920 || options.outHeight / options.inSampleSize > 1080) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false; options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }
    static JSONArray catalog(Context c) throws IOException, JSONException {
        try (InputStream in = c.getAssets().open("mosques.json"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new JSONArray(out.toString(StandardCharsets.UTF_8.name()));
        }
    }
    static String credits(Context c) {
        StringBuilder result = new StringBuilder("Foto masjid daripada Wikimedia Commons. Paparan dipotong mengikut skrin dan digelapkan; fail asal tidak diubah.\n\n");
        try {
            JSONArray items = catalog(c);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                result.append(item.getString("name")).append("\n").append(item.getString("author")).append(" • ").append(item.getString("license"))
                    .append("\n").append(item.getString("source")).append("\n").append(item.getString("licenseUrl")).append("\n\n");
            }
        } catch (Exception ignored) { result.append("Kredit foto tidak dapat dibaca."); }
        return result.toString();
    }
    void stop() { stopped = true; handler.removeCallbacksAndMessages(null); worker.shutdownNow(); screen.setBackgroundPhoto(null, ""); }
}
