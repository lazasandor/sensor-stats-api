package dev.sandor.sensor_stats.query;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Query result, serialised directly as the JSON response.
 */
public record StatisticsResult(String statistic, List<DeviceStatistics> devices) {

    public record DeviceStatistics(
            String deviceId,
            Instant from,
            Instant to,
            int readingCount,
            Map<String, MetricValue> metrics) {

        public record MetricValue(Double value, String unit, long sampleCount) {

        }
    }
}
