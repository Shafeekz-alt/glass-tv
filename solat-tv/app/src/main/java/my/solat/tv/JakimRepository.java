package my.solat.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/** Only dated, zone-validated records from the HTTPS JAKIM endpoint enter the cache. */
public final class JakimRepository {
    public interface Listener { void changed(List<PrayerSchedule.Day> days, String status); }
    private final SharedPreferences cache;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private boolean busy;
    private long lastFetch;
    private long refreshAfter = 15 * 60_000L;
    public JakimRepository(Context c) { cache = c.getSharedPreferences("jakim-v1", Context.MODE_PRIVATE); }
    public void refresh(boolean force, Listener listener) {
        if (busy || closed) return;
        if (!force && System.currentTimeMillis() - lastFetch < refreshAfter) return;
        busy = true; lastFetch = System.currentTimeMillis();
        worker.execute(() -> {
            List<PrayerSchedule.Day> days = readCache();
            deliver(listener, days, days.isEmpty() ? "Memuatkan jadual JAKIM…" : "Jadual JAKIM tersimpan • sedang disemak");
            boolean currentOkay = false;
            boolean nextOkay = false;
            YearMonth month = YearMonth.now(PrayerSchedule.ZONE);
            for (int i = 0; i < 2 && !closed; i++) {
                YearMonth requested = month.plusMonths(i);
                try {
                    String payload = fetch(requested);
                    List<PrayerSchedule.Day> parsed = parse(payload, requested);
                    if (parsed.isEmpty()) throw new IOException("Empty month");
                    cache.edit().putString(requested.toString(), payload).apply();
                    if (i == 0) currentOkay = true; else nextOkay = true;
                } catch (Exception ignored) { /* Keep dated cache; UI explicitly reports cached/unavailable data. */ }
            }
            List<PrayerSchedule.Day> result = readCache();
            boolean today = PrayerSchedule.today(result, Instant.now()) != null;
            String status = currentOkay && today ? "Dikemas kini daripada e-Solat JAKIM" :
                today ? "JAKIM tidak dapat dicapai • menggunakan jadual tersimpan" : "Jadual hari ini belum tersedia • semak internet dan tarikh TV";
            if (currentOkay && !nextOkay) status += " • bulan berikutnya belum dikemas kini";
            deliver(listener, result, status);
            final boolean fetchedCurrent = currentOkay && today;
            main.post(() -> { busy = false; refreshAfter = fetchedCurrent ? 6 * 60 * 60_000L : 15 * 60_000L; });
        });
    }
    private void deliver(Listener listener, List<PrayerSchedule.Day> days, String status) {
        main.post(() -> { if (!closed) listener.changed(days, status); });
    }
    private List<PrayerSchedule.Day> readCache() {
        List<PrayerSchedule.Day> result = new ArrayList<>();
        YearMonth month = YearMonth.now(PrayerSchedule.ZONE);
        for (int i = 0; i < 2; i++) {
            YearMonth m = month.plusMonths(i);
            try { result.addAll(parse(cache.getString(m.toString(), ""), m)); } catch (Exception ignored) {}
        }
        // Keep a bounded cache: the current and next calendar month only.
        for (String key : cache.getAll().keySet()) {
            if (!key.equals(month.toString()) && !key.equals(month.plusMonths(1).toString())) cache.edit().remove(key).apply();
        }
        result.sort(Comparator.comparing(d -> d.date));
        return Collections.unmodifiableList(result);
    }
    static List<PrayerSchedule.Day> parse(String payload, YearMonth requested) throws JSONException {
        JSONObject root = new JSONObject(payload);
        if (!PrayerSchedule.ZONE_CODE.equals(root.optString("zone"))) throw new JSONException("Unexpected zone");
        if (!root.optString("status").toUpperCase(Locale.ROOT).startsWith("OK")) throw new JSONException("JAKIM returned an error");
        JSONArray rows = root.getJSONArray("prayerTime");
        List<PrayerSchedule.Day> result = new ArrayList<>();
        Set<LocalDate> seen = new HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String rawDate = row.getString("date");
            LocalDate date = JakimDate.parse(rawDate);
            if (!YearMonth.from(date).equals(requested)) throw new JSONException("Unexpected date");
            if (!seen.add(date)) throw new JSONException("Duplicate date");
            Map<String, LocalTime> times = new LinkedHashMap<>();
            for (String key : PrayerSchedule.KEYS) times.put(key, LocalTime.parse(row.getString(key), DateTimeFormatter.ISO_LOCAL_TIME));
            result.add(new PrayerSchedule.Day(date, row.optString("hijri"), times));
        }
        if (result.size() != requested.lengthOfMonth()) throw new JSONException("Incomplete month");
        return result;
    }
    private static String fetch(YearMonth month) throws IOException {
        URL url = new URL("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=" + PrayerSchedule.ZONE_CODE + "&year=" + month.getYear() + "&month=" + month.getMonthValue());
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(12_000); c.setReadTimeout(12_000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "WaktuSolatTV/1.0 (personal Android TV screensaver)");
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = in.read(buffer)) != -1) {
                    if (out.size() + count > 1_048_576) throw new IOException("Response too large");
                    out.write(buffer, 0, count);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally { c.disconnect(); }
    }
    public void close() { closed = true; worker.shutdownNow(); }
}
