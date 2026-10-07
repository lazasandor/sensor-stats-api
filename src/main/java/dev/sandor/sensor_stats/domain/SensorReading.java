package dev.sandor.sensor_stats.domain;

import java.time.Instant;
import java.util.Objects;

/**
 *  A single reading reported by a device at a time.
 */
public record SensorReading(
        String deviceId,
        Instant measureTime,
        Double temperatureCelsius,
        Double humidity) {

    public SensorReading {
        Objects.requireNonNull(deviceId, "deviceId must not be null");
        Objects.requireNonNull(measureTime, "measureTime must not be null");
    }
}
