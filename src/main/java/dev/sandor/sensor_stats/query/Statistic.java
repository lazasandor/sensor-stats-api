package dev.sandor.sensor_stats.query;

/**
 * Statistics that can be queried for readings.
 */
public enum Statistic {
    MIN("min"), MAX("max"), AVG("avg");

    private final String key;

    Statistic(String key) { this.key = key; }

    public String key() { return key; }

    public static Statistic fromKey(String key) {
        for (Statistic statistic : values()) {
            if (statistic.key.equalsIgnoreCase(key.trim())) {
                return statistic;
            }
        }
        throw new IllegalArgumentException("Unknown statistic: '" + key + "'");
    }
}
