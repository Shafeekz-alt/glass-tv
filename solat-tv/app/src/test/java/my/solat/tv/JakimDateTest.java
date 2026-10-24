package my.solat.tv;

import org.junit.Test;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import static org.junit.Assert.assertEquals;

public class JakimDateTest {
    @Test public void malayMonths() {
        String[] months = {"Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ogo", "Sep", "Okt", "Nov", "Dis"};
        for (int i = 0; i < months.length; i++)
            assertEquals(LocalDate.of(2026, i + 1, 1), JakimDate.parse("01-" + months[i] + "-2026"));
    }
    @Test public void englishAndIsoStillWork() {
        assertEquals(LocalDate.of(2026, 10, 24), JakimDate.parse("24-Oct-2026"));
        assertEquals(LocalDate.of(2026, 10, 24), JakimDate.parse("2026-10-24"));
    }
    @Test public void caseAndLeapDay() {
        assertEquals(LocalDate.of(2026, 10, 24), JakimDate.parse("24-OKT-2026"));
        assertEquals(LocalDate.of(2028, 2, 29), JakimDate.parse("29-Feb-2028"));
    }
    @Test(expected = DateTimeParseException.class) public void invalidDayRejected() {
        JakimDate.parse("31-Apr-2026");
    }
}
