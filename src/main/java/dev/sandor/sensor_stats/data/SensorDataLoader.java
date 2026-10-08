package dev.sandor.sensor_stats.data;

import dev.sandor.sensor_stats.domain.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads the CSV once at startup. If the file cannot be read the application fails to start.
 */
@Component
@EnableConfigurationProperties(SensorDataProperties.class)
public class SensorDataLoader {

    private static final Logger log = LoggerFactory.getLogger(SensorDataLoader.class);

    private final Map<String, List<SensorReading>> readingsByDevice;

    @Autowired
    public SensorDataLoader(SensorDataProperties properties) {
        this(readCsv(properties.location()));
    }

    private SensorDataLoader(List<SensorReading> readings) {
        this.readingsByDevice = Map.copyOf(readings.stream()
                .collect(Collectors.groupingBy(
                        SensorReading::deviceId,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(SensorReading::measureTime))
                                        .toList()))));
    }

    private static List<SensorReading> readCsv(Resource location) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(location.getInputStream(), StandardCharsets.UTF_8))) {
            List<SensorReading> result = new CsvReadingParser().parse(reader);

            log.info("Loaded {} readings from {}", result.size(), location);
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read sensor data from " + location, e);
        }
    }

    public Set<String> findAllDeviceIds() {
        return readingsByDevice.keySet();
    }

    public List<SensorReading> findByDeviceId(String deviceId) {
        return readingsByDevice.getOrDefault(deviceId, List.of());
    }
}
