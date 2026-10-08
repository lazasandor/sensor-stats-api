package dev.sandor.sensor_stats.query;

import dev.sandor.sensor_stats.data.SensorDataLoader;
import dev.sandor.sensor_stats.data.SensorDataProperties;
import dev.sandor.sensor_stats.query.ReadingStatisticsService.DeviceResult;
import dev.sandor.sensor_stats.query.ReadingStatisticsService.MetricResult;
import dev.sandor.sensor_stats.query.ReadingStatisticsService.StatisticsResult;
import dev.sandor.sensor_stats.query.exception.InvalidTimeRangeException;
import dev.sandor.sensor_stats.query.exception.UnknownDeviceException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses real data (the sample from the task) loaded from memory instead of mocks.
 */
class ReadingStatisticsServiceTest {

    private static final String CSV = """
            DeviceId,MeasureTime,Temperature,TempUnit,Humidity
            1,2026-07-10T10:00:00Z,23,C,42
            1,2026-07-10T14:00:00Z,32,C,45
            1,2026-07-10T18:00:00Z,20,C,40
            1,2026-07-11T10:00:00Z,22,C,44
            2,2026-07-10T10:00:00Z,77,F,
            2,2026-07-10T12:00:00Z,80,F,
            """;

    private static final Instant JULY_10 = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant JULY_11 = Instant.parse("2026-07-11T00:00:00Z");
    private static final Instant JULY_12 = Instant.parse("2026-07-12T00:00:00Z");

    private final ReadingStatisticsService service = new ReadingStatisticsService(loaderOf(CSV));

    @Test
    void computesMinMaxAndAverageWithinRange() {
        assertThat(device1TemperatureOnJuly10(Statistic.MIN).value()).isEqualTo(20.0);
        assertThat(device1TemperatureOnJuly10(Statistic.MAX).value()).isEqualTo(32.0);
        assertThat(device1TemperatureOnJuly10(Statistic.AVG).value()).isEqualTo(25.0);
        assertThat(device1TemperatureOnJuly10(Statistic.AVG).sampleCount()).isEqualTo(3);
    }

    @Test
    void usesLatestUtcDayOfEachDeviceWhenNoRangeGiven() {
        StatisticsResult result = service.queryStatistics(null, null, null, null, null);

        DeviceResult device1 = result.devices().get(0);
        assertThat(device1.deviceId()).isEqualTo("1");
        assertThat(device1.from()).isEqualTo(JULY_11);
        assertThat(device1.to()).isEqualTo(JULY_12);
        assertThat(device1.readingCount()).isEqualTo(1);
        assertThat(device1.metrics().get("temperature").value()).isEqualTo(22.0);

        // Device 2 has no data on July 11, so its own latest day is used
        DeviceResult device2 = result.devices().get(1);
        assertThat(device2.deviceId()).isEqualTo("2");
        assertThat(device2.from()).isEqualTo(JULY_10);
        assertThat(device2.readingCount()).isEqualTo(2);
    }

    @Test
    void appliesDefaultsWhenParametersAreMissing() {
        StatisticsResult result = service.queryStatistics(null, null, null, null, null);

        assertThat(result.statistic()).isEqualTo("avg");
        assertThat(result.devices()).extracting(DeviceResult::deviceId).containsExactly("1", "2");
        assertThat(result.devices().getFirst().metrics()).containsOnlyKeys("temperature", "humidity");
    }

    @Test
    void treatsEmptyAndBlankParametersAsNotGiven() {
        StatisticsResult result = service.queryStatistics(List.of(" ", ""), null, null, List.of(), null);

        assertThat(result.devices()).extracting(DeviceResult::deviceId).containsExactly("1", "2");
        assertThat(result.devices().getFirst().metrics()).containsOnlyKeys("temperature", "humidity");
    }

    @Test
    void normalisesRequestedDeviceIds() {
        StatisticsResult result = service.queryStatistics(List.of("2", " 1 ", "2"), null, null, null, null);

        assertThat(result.devices()).extracting(DeviceResult::deviceId).containsExactly("1", "2");
    }

    @Test
    void returnsOnlyRequestedMetrics() {
        StatisticsResult result = service.queryStatistics(
                List.of("1"), null, null, List.of(Metric.HUMIDITY), Statistic.MAX);

        assertThat(result.statistic()).isEqualTo("max");
        assertThat(result.devices().getFirst().metrics()).containsOnlyKeys("humidity");
    }

    @Test
    void convertsFahrenheitBeforeAggregating() {
        StatisticsResult result = service.queryStatistics(
                List.of("2"), JULY_10, JULY_11, List.of(Metric.TEMPERATURE), Statistic.AVG);

        MetricResult temperature = result.devices().getFirst().metrics().get("temperature");
        assertThat(temperature.value()).isEqualTo(25.83); // (25.0 + 26.67) / 2, rounded
        assertThat(temperature.unit()).isEqualTo("°C");
    }

    @Test
    void returnsNullInsteadOfZeroWhenMetricHasNoSamples() {
        StatisticsResult result = service.queryStatistics(
                List.of("2"), null, null, List.of(Metric.HUMIDITY), null);

        MetricResult humidity = result.devices().getFirst().metrics().get("humidity");
        assertThat(humidity.value()).isNull();
        assertThat(humidity.sampleCount()).isZero();
    }

    @Test
    void excludesReadingAtEndOfRange() {
        StatisticsResult result = service.queryStatistics(
                List.of("1"), JULY_10, Instant.parse("2026-07-10T14:00:00Z"), null, null);

        // only the 10:00 reading; the 14:00 reading lies exactly on the exclusive end
        assertThat(result.devices().getFirst().readingCount()).isEqualTo(1);
    }

    @Test
    void acceptsOpenEndedRange() {
        StatisticsResult onlyFrom = service.queryStatistics(
                List.of("1"), Instant.parse("2026-07-10T14:00:00Z"), null, null, null);
        StatisticsResult onlyTo = service.queryStatistics(
                List.of("1"), null, Instant.parse("2026-07-10T14:00:00Z"), null, null);

        assertThat(onlyFrom.devices().getFirst().readingCount()).isEqualTo(3);
        assertThat(onlyTo.devices().getFirst().readingCount()).isEqualTo(1);
    }

    @Test
    void returnsEmptyResultForRangeWithoutData() {
        StatisticsResult result = service.queryStatistics(List.of("1"),
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z"), null, null);

        DeviceResult device = result.devices().getFirst();
        assertThat(device.readingCount()).isZero();
        assertThat(device.metrics().get("temperature").value()).isNull();
    }

    @Test
    void reportsAllUnknownDevices() {
        assertThatThrownBy(() -> service.queryStatistics(List.of("1", "9", "42"), null, null, null, null))
                .isInstanceOfSatisfying(UnknownDeviceException.class,
                        e -> assertThat(e.getDeviceIds()).containsExactlyInAnyOrder("9", "42"));
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> service.queryStatistics(null, JULY_11, JULY_10, null, null))
                .isInstanceOf(InvalidTimeRangeException.class);
    }

    private MetricResult device1TemperatureOnJuly10(Statistic statistic) {
        StatisticsResult result = service.queryStatistics(
                List.of("1"), JULY_10, JULY_11, List.of(Metric.TEMPERATURE), statistic);
        return result.devices().getFirst().metrics().get("temperature");
    }

    private static SensorDataLoader loaderOf(String csv) {
        return new SensorDataLoader(new SensorDataProperties(
                new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8))));
    }
}
