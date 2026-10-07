package dev.sandor.sensor_stats.query;

/**
 * Metrics that can be queried for statistics.
 */
public enum Metric {
    TEMPERATURE("temperature", "°C"),
    HUMIDITY("humidity", "%");

    private final String key;
    private final String unit;

    Metric(String key, String unit) {
        this.key = key;
        this.unit = unit;
    }

    public String key() {
        return key;
    }

    public String unit() {
        return unit;
    }

    public static Metric fromKey(String key) {
        for (Metric metric : values()) {
            if (metric.key.equalsIgnoreCase(key.trim())) {
                return metric;
            }
        }
        throw new IllegalArgumentException("Unknown metric: '" + key + "'");
    }
}
