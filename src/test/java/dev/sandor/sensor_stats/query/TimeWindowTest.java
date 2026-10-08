package dev.sandor.sensor_stats.query;

import dev.sandor.sensor_stats.query.exception.InvalidTimeRangeException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeWindowTest {

    private static final Instant TEN = Instant.parse("2026-07-10T10:00:00Z");
    private static final Instant TWELVE = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void includesStartAndExcludesEnd() {
        TimeWindow window = new TimeWindow(TEN, TWELVE);

        assertThat(window.contains(TEN)).isTrue();
        assertThat(window.contains(Instant.parse("2026-07-10T11:59:59Z"))).isTrue();
        assertThat(window.contains(TWELVE)).isFalse();
        assertThat(window.contains(Instant.parse("2026-07-10T09:59:59Z"))).isFalse();
    }

    @Test
    void treatsMissingBoundsAsUnbounded() {
        assertThat(new TimeWindow(TEN, null).contains(Instant.parse("2030-01-01T00:00:00Z"))).isTrue();
        assertThat(new TimeWindow(null, TWELVE).contains(Instant.parse("2000-01-01T00:00:00Z"))).isTrue();
        assertThat(new TimeWindow(null, null).isUnbounded()).isTrue();
        assertThat(new TimeWindow(TEN, null).isUnbounded()).isFalse();
    }

    @Test
    void rejectsFromNotBeforeTo() {
        assertThatThrownBy(() -> new TimeWindow(TWELVE, TEN))
                .isInstanceOf(InvalidTimeRangeException.class);
        assertThatThrownBy(() -> new TimeWindow(TEN, TEN))
                .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void utcDayCoversWholeCalendarDay() {
        TimeWindow day = TimeWindow.utcDayOf(Instant.parse("2026-07-10T23:59:59Z"));

        assertThat(day.from()).isEqualTo(Instant.parse("2026-07-10T00:00:00Z"));
        assertThat(day.to()).isEqualTo(Instant.parse("2026-07-11T00:00:00Z"));
    }
}
