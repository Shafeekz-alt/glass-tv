package my.solat.tv;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class PrayerScheduleTest {
    // Synthetic fixtures for scheduling tests only; these are NOT actual prayer data.
    private static PrayerSchedule.Day syntheticDay(String date) {
        return new PrayerSchedule.Day(LocalDate.parse(date), "synthetic", syntheticTimes());
    }

    private static Map<String, LocalTime> syntheticTimes() {
        Map<String, LocalTime> times = new LinkedHashMap<>();
        times.put("fajr", LocalTime.of(6, 0));
        times.put("syuruk", LocalTime.of(7, 0));
        times.put("dhuhr", LocalTime.of(13, 0));
        times.put("asr", LocalTime.of(16, 0));
        times.put("maghrib", LocalTime.of(19, 0));
        times.put("isha", LocalTime.of(20, 0));
        return times;
    }

    private static Instant malaysia(String dateTime) {
        return java.time.LocalDateTime.parse(dateTime)
                .atZone(ZoneId.of("Asia/Kuala_Lumpur")).toInstant();
    }

    @Test public void usesMalaysiaDateAndEventInstantWhenHostTimezoneDiffers() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            PrayerSchedule.Day day = syntheticDay("2026-10-24");
            List<PrayerSchedule.Day> days = Arrays.asList(syntheticDay("2026-10-23"), day);
            Instant now = Instant.parse("2026-10-23T22:00:00Z");
            assertSame(day, PrayerSchedule.today(days, now));
            PrayerSchedule.Event next = PrayerSchedule.next(days, now);
            assertEquals("fajr", next.key);
            assertEquals(now, next.at);
            assertEquals("TRG01:2026-10-24:fajr", next.id);
            assertEquals("Subuh", next.label);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test public void syurukNeverBecomesNextOrDue() {
        List<PrayerSchedule.Day> days = Collections.singletonList(syntheticDay("2026-10-24"));
        assertEquals("dhuhr", PrayerSchedule.next(days, malaysia("2026-10-24T06:00:01")).key);
        Instant syuruk = malaysia("2026-10-24T07:00:00");
        assertEquals("dhuhr", PrayerSchedule.next(days, syuruk).key);
        assertNull(PrayerSchedule.due(days, syuruk, syuruk.minusSeconds(60), Collections.emptySet()));
    }

    @Test public void afterIshaFindsTomorrowFajrAcrossMonthRolloverWithUnsortedDays() {
        List<PrayerSchedule.Day> days = Arrays.asList(
                syntheticDay("2026-11-02"), syntheticDay("2026-11-01"), syntheticDay("2026-10-31"));
        PrayerSchedule.Event next = PrayerSchedule.next(days, malaysia("2026-10-31T20:00:01"));
        assertEquals("fajr", next.key);
        assertEquals(malaysia("2026-11-01T06:00:00"), next.at);
        assertEquals("TRG01:2026-11-01:fajr", next.id);
        assertNull(PrayerSchedule.next(Collections.singletonList(syntheticDay("2026-10-31")),
                malaysia("2026-10-31T20:00:01")));
    }

    @Test public void exactPrayerIsDueOnceUsingDeliveredIds() {
        List<PrayerSchedule.Day> days = Collections.singletonList(syntheticDay("2026-10-24"));
        Instant at = malaysia("2026-10-24T13:00:00");
        Set<String> delivered = new HashSet<>();
        PrayerSchedule.Event event = PrayerSchedule.due(days, at, at, delivered);
        assertNotNull(event);
        assertEquals("dhuhr", event.key);
        assertEquals(at, event.at);
        delivered.add(event.id);
        assertNull(PrayerSchedule.due(days, at, at, delivered));
        assertNull(PrayerSchedule.due(days, at.plusSeconds(10), at, delivered));
    }

    @Test public void resumeAfterPrayerDoesNotCatchUpEvenWithinGrace() {
        List<PrayerSchedule.Day> days = Collections.singletonList(syntheticDay("2026-10-24"));
        Instant at = malaysia("2026-10-24T16:00:00");
        assertNull(PrayerSchedule.due(days, at.plusSeconds(10), at.plusSeconds(1), Collections.emptySet()));
        assertNull(PrayerSchedule.due(days, at.plusSeconds(10), at.plusNanos(1), Collections.emptySet()));
    }

    @Test public void dueGraceIncludesThirtySecondsButNothingLaterOrBeforePrayer() {
        List<PrayerSchedule.Day> days = Collections.singletonList(syntheticDay("2026-10-24"));
        Instant at = malaysia("2026-10-24T19:00:00");
        Instant start = at.minusSeconds(60);
        assertNull(PrayerSchedule.due(days, at.minusNanos(1), start, Collections.emptySet()));
        assertNotNull(PrayerSchedule.due(days, at.plusSeconds(30), start, Collections.emptySet()));
        assertNull(PrayerSchedule.due(days, at.plusSeconds(30).plusNanos(1), start, Collections.emptySet()));
        assertNull(PrayerSchedule.due(days, at.plusSeconds(31), start, Collections.emptySet()));
    }

    @Test public void stalePreviousDayNeverTriggers() {
        List<PrayerSchedule.Day> stale = Collections.singletonList(syntheticDay("2026-10-23"));
        Instant now = malaysia("2026-10-24T06:00:00");
        assertNull(PrayerSchedule.today(stale, now));
        assertNull(PrayerSchedule.due(stale, now, now.minusSeconds(86400), Collections.emptySet()));
        assertNull(PrayerSchedule.next(stale, now));
    }

    @Test public void rejectsMissingAndNullPrayerTimes() {
        Map<String, LocalTime> missing = syntheticTimes();
        missing.remove("asr");
        assertThrows(NullPointerException.class,
                () -> new PrayerSchedule.Day(LocalDate.of(2026, 10, 24), "", missing));
        Map<String, LocalTime> nullTime = syntheticTimes();
        nullTime.put("isha", null);
        assertThrows(NullPointerException.class,
                () -> new PrayerSchedule.Day(LocalDate.of(2026, 10, 24), "", nullTime));
    }

    @Test public void rejectsDescendingOrEqualPrayerTimes() {
        Map<String, LocalTime> descending = syntheticTimes();
        descending.put("asr", LocalTime.of(12, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PrayerSchedule.Day(LocalDate.of(2026, 10, 24), "", descending));
        Map<String, LocalTime> equal = syntheticTimes();
        equal.put("isha", equal.get("maghrib"));
        assertThrows(IllegalArgumentException.class,
                () -> new PrayerSchedule.Day(LocalDate.of(2026, 10, 24), "", equal));
    }
}
