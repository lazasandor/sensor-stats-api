package dev.sandor.sensor_stats.query;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Statistics that can be queried for readings.
 */
public enum Statistic {
    MIN("min"), MAX("max"), AVG("avg");

    private final String key;

    Statistic(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Statistic fromKey(String key) {
        for (Statistic statistic : values()) {
            if (statistic.key.equalsIgnoreCase(key.trim())) {
                return statistic;
            }
        }
        throw new IllegalArgumentException("Unknown statistic: '" + key + "' allowed values: " + allowedValues());
    }

    private static String allowedValues() {
        return Arrays.stream(values())
                .map(Statistic::key)
                .collect(Collectors.joining(", "));
    }
}
