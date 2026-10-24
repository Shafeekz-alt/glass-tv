package my.solat.tv;

import java.time.*;
import java.util.*;

/** Prayer events always use Malaysia's civil time, independently of the TV timezone. */
public final class PrayerSchedule {
    public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    public static final String ZONE_CODE = "TRG01";
    public static final String LOCATION = "Kuala Terengganu";
    public static final String[] KEYS = {"fajr", "syuruk", "dhuhr", "asr", "maghrib", "isha"};
    public static final String[] LABELS = {"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};
    private PrayerSchedule() {}
    public static final class Day {
        public final LocalDate date;
        public final String hijri;
        public final Map<String, LocalTime> times;
        public Day(LocalDate date, String hijri, Map<String, LocalTime> times) {
            this.date = Objects.requireNonNull(date);
            this.hijri = hijri == null ? "" : hijri;
            LinkedHashMap<String, LocalTime> copy = new LinkedHashMap<>();
            LocalTime previous = null;
            for (String key : KEYS) {
                LocalTime t = Objects.requireNonNull(times.get(key), "Missing " + key);
                if (previous != null && !t.isAfter(previous)) throw new IllegalArgumentException("Unordered prayer times");
                copy.put(key, t); previous = t;
            }
            this.times = Collections.unmodifiableMap(copy);
        }
    }
    public static final class Event {
        public final String key, label, id;
        public final Instant at;
        public Event(Day day, int index) {
            key = KEYS[index]; label = LABELS[index];
            at = day.date.atTime(day.times.get(key)).atZone(ZONE).toInstant();
            id = ZONE_CODE + ":" + day.date + ":" + key;
        }
    }
    public static Day today(List<Day> days, Instant now) {
        LocalDate date = now.atZone(ZONE).toLocalDate();
        for (Day day : days) if (day.date.equals(date)) return day;
        return null;
    }
    public static Event next(List<Day> days, Instant now) {
        Event best = null;
        for (Day day : days) for (int i = 0; i < KEYS.length; i++) {
            if (i == 1) continue; // Syuruk is displayed, but never triggers azan/countdown.
            Event e = new Event(day, i);
            if (!e.at.isBefore(now) && (best == null || e.at.isBefore(best.at))) best = e;
        }
        return best;
    }
    public static Event due(List<Day> days, Instant now, Instant sessionStart, Set<String> delivered) {
        Day today = today(days, now);
        if (today == null) return null;
        for (int i = 0; i < KEYS.length; i++) {
            if (i == 1) continue;
            Event e = new Event(today, i);
            if (!now.isBefore(e.at) && !now.isAfter(e.at.plusSeconds(30)) && !e.at.isBefore(sessionStart) && !delivered.contains(e.id)) return e;
        }
        return null;
    }
}
