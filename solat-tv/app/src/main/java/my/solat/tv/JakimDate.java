package my.solat.tv;

import java.time.LocalDate;
import java.time.format.*;
import java.util.Locale;

/** JAKIM can return Malay month abbreviations independently of device locale. */
final class JakimDate {
    private static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
        .parseCaseInsensitive().appendPattern("dd-MMM-uuuu")
        .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);

    static LocalDate parse(String value) {
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(value);
        String normalized = value.toLowerCase(Locale.ROOT)
            .replace("-mac-", "-mar-").replace("-mei-", "-may-")
            .replace("-ogo-", "-aug-").replace("-okt-", "-oct-")
            .replace("-dis-", "-dec-");
        return LocalDate.parse(normalized, FORMAT);
    }
}
