package dev.sandor.sensor_stats.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public record TimeWindow(Instant from, Instant to) {
    public TimeWindow {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
    }

    public boolean isUnbounded() {
        return from == null && to == null;
    }

    public boolean contains(Instant time) {
        return (from == null || !time.isBefore(from))
                && (to == null || time.isBefore(to));
    }

    public static TimeWindow utcDayOf(Instant instant) {
        LocalDate day = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return new TimeWindow(
                day.atStartOfDay(ZoneOffset.UTC).toInstant(),
                day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
