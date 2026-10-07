package dev.sandor.sensor_stats.domain;

/**
 * Temperature units that can appear in the source data.
 */
public enum TemperatureUnit {

    CELSIUS("C"),
    FAHRENHEIT("F");

    private final String symbol;

    TemperatureUnit(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Converts the given temperature value to Celsius.
     * This method is necessary because the application calculates averages.
     */
    public double toCelsius(double value) {
        return switch (this) {
            case CELSIUS -> value;
            case FAHRENHEIT -> (value - 32) * 5 / 9;
        };
    }

    public static TemperatureUnit fromSymbol(String symbol) {
        for (TemperatureUnit unit : values()) {
            if (unit.symbol.equalsIgnoreCase(symbol.trim())) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown temperature unit: '" + symbol + "'");
    }

}
