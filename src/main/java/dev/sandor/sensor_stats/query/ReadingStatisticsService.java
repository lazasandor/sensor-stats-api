package dev.sandor.sensor_stats.query;

import dev.sandor.sensor_stats.data.SensorDataLoader;
import dev.sandor.sensor_stats.domain.SensorReading;
import dev.sandor.sensor_stats.query.exception.UnknownDeviceException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ReadingStatisticsService {

    private final SensorDataLoader loader;

    public ReadingStatisticsService(SensorDataLoader loader) {
        this.loader = loader;
    }

    public StatisticsResult queryStatistics(List<String> deviceIds,
                                            Instant from,
                                            Instant to,
                                            List<Metric> metrics,
                                            Statistic statistic) {
        TimeWindow requested = new TimeWindow(from, to);
        Statistic resolvedStatistic = statistic != null ? statistic : Statistic.AVG;
        List<Metric> resolvedMetrics = (metrics != null && !metrics.isEmpty())
                ? metrics.stream().distinct().toList()
                : List.of(Metric.values());

        List<DeviceResult> devices = resolveDevices(deviceIds).stream()
                .map(id -> forDevice(id, requested, resolvedMetrics, resolvedStatistic))
                .toList();
        return new StatisticsResult(resolvedStatistic.key(), devices);
    }

    private List<String> resolveDevices(List<String> requested) {
        Set<String> known = loader.findAllDeviceIds();
        List<String> ids = requested == null ? List.of() : requested.stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .sorted()
                .toList();

        if (ids.isEmpty()) {
            return known.stream().sorted().toList();
        }

        List<String> unknown = ids.stream().filter(id -> !known.contains(id)).toList();
        if (!unknown.isEmpty()) {
            throw new UnknownDeviceException(unknown);
        }
        return ids;
    }

    private DeviceResult forDevice(String deviceId, TimeWindow requested,
                                   List<Metric> metrics, Statistic statistic) {
        List<SensorReading> all = loader.findByDeviceId(deviceId);

        // No range given: the latest UTC day on which this device reported.
        TimeWindow window = requested.isUnbounded() && !all.isEmpty()
                ? TimeWindow.utcDayOf(all.getLast().measureTime())
                : requested;

        List<SensorReading> selected = all.stream()
                .filter(r -> window.contains(r.measureTime()))
                .toList();

        Map<String, MetricResult> results = new HashMap<>();
        for (Metric metric : metrics) {
            results.put(metric.key(), compute(metric, statistic, selected));
        }
        return new DeviceResult(deviceId, window.from(), window.to(), selected.size(), results);
    }

    private static MetricResult compute(
            Metric metric,
            Statistic statistic,
            List<SensorReading> readings) {

        List<Double> values = new ArrayList<>();

        for (SensorReading reading : readings) {
            Double value = switch (metric) {
                case TEMPERATURE -> reading.temperatureCelsius();
                case HUMIDITY -> reading.humidity();
            };

            if (value != null) {
                values.add(value);
            }
        }

        if (values.isEmpty()) {
            return new MetricResult(null, metric.unit(), 0);
        }

        double result = calculateMetricResult(statistic, values);

        return new MetricResult(
                round(result),
                metric.unit(),
                values.size()
        );
    }

    private static double calculateMetricResult(
            Statistic statistic,
            List<Double> values) {

        return switch (statistic) {
            case MIN -> values.stream()
                    .min(Double::compare)
                    .get();

            case MAX -> values.stream()
                    .max(Double::compare)
                    .get();

            case AVG -> values.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .getAsDouble();
        };
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    public record StatisticsResult(String statistic, List<DeviceResult> devices) {
    }

    public record DeviceResult(String deviceId, Instant from, Instant to,
                               int readingCount, Map<String, MetricResult> metrics) {
    }

    public record MetricResult(Double value, String unit, long sampleCount) {
    }

}
