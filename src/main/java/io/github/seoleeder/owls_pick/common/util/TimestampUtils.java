package io.github.seoleeder.owls_pick.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimestampUtils {

    /**
     * Unix TimeStamp (second) -> LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestamp) {
        if (timestamp <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * Unix TimeStamp (second) -> LocalDate
     * */
    public static LocalDate toLocalDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return null;
        return LocalDate.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault() // KST
        );
    }

    /**
     * LocalDateTime -> Unix Timestamp (second)
     * */
    public static long toEpoch(LocalDateTime dateTime) {
        if (dateTime == null) return 0L;
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * LocalDate -> Unix Timestamp (second)
     * */
    public static long toEpoch(LocalDate date) {
        if (date == null) return 0L;
        return date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

}
