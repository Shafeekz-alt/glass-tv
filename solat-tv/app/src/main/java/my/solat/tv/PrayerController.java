package my.solat.tv;

import android.content.*;
import android.os.*;
import java.time.*;
import java.util.*;

/** One controller per visible dream/preview; preview never emits scheduled audio. */
final class PrayerController {
    private final SharedPreferences settings;
    private final PrayerScreen screen;
    private final JakimRepository repository;
    private final AzanPlayer player;
    private final PhotoRepository photos;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final boolean allowSound;
    private final Context context;
    private List<PrayerSchedule.Day> days = Collections.emptyList();
    private String status = "Memuatkan jadual JAKIM…";
    private Instant started;
    private LocalDate displayedDate;
    private boolean running;
    private Set<String> delivered;
    PrayerController(Context c, PrayerScreen view, boolean sound) {
        context = c; screen = view; allowSound = sound;
        settings = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
        repository = new JakimRepository(c); player = new AzanPlayer(c); photos = new PhotoRepository(c, view);
    }
    void start() {
        started = Instant.now(); running = true; photos.start();
        delivered = new HashSet<>(settings.getStringSet("delivered", Collections.emptySet()));
        LocalDate oldest = started.atZone(PrayerSchedule.ZONE).toLocalDate().minusDays(1);
        delivered.removeIf(id -> { try { return LocalDate.parse(id.split(":")[1]).isBefore(oldest); } catch (Exception ex) { return true; } });
        handler.post(tick);
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            Instant now = Instant.now();
            LocalDate today = now.atZone(PrayerSchedule.ZONE).toLocalDate();
            boolean dateChanged = !today.equals(displayedDate);
            displayedDate = today;
            repository.refresh(dateChanged, (fresh, message) -> { days = fresh; status = message; });
            boolean enabled = settings.getBoolean("sound", true) && AzanPlayer.available(context);
            if (enabled && allowSound) {
                PrayerSchedule.Event event = PrayerSchedule.due(days, now, started, delivered);
                if (event != null) {
                    delivered.add(event.id);
                    // Commit before starting audio: reconnect/restart cannot replay this event.
                    if (settings.edit().putStringSet("delivered", new HashSet<>(delivered)).commit())
                        player.play(() -> status = "Audio azan tidak dapat dimainkan • semak pilihan audio dan kelantangan");
                }
            }
            PrayerSchedule.Day day = PrayerSchedule.today(days, now);
            PrayerSchedule.Event next = day == null ? null : PrayerSchedule.next(days, now);
            screen.show(day, next, now, status, enabled && allowSound);
            handler.postDelayed(this, 1000);
        }
    };
    void stop() { running = false; handler.removeCallbacksAndMessages(null); repository.close(); player.stop(); photos.stop(); }
}
