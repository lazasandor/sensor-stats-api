package dev.sandor.sensor_stats.data;

import dev.sandor.sensor_stats.domain.SensorReading;
import dev.sandor.sensor_stats.domain.TemperatureUnit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;

/**
 * Parses sensor readings from CSV.
 */
public class CsvReadingParser {

    static final String DEVICE_ID = "DeviceId";
    static final String MEASURE_TIME = "MeasureTime";
    static final String TEMPERATURE = "Temperature";
    static final String TEMP_UNIT = "TempUnit";
    static final String HUMIDITY = "Humidity";

    public List<SensorReading> parse(BufferedReader reader) {
        try {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV is empty, header row expected.");
            }

            Map<String, Integer> columns = indexColumns(headerLine);
            List<SensorReading> readings = new ArrayList<>();
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) continue;

                readings.add(parseRow(line, columns));
            }

            return readings;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV", e);
        }
    }

    private SensorReading parseRow(String line, Map<String, Integer> columns) {
        // limit -1 keeps trailing empty fields, e.g. a missing humidity at the end of the row
        String[] fields = line.split(",", -1);
        if (fields.length < columns.size()) {
            throw new IllegalArgumentException("expected " + columns.size()
                    + " fields but found " + fields.length + ": " + Arrays.toString(fields));
        }

        String deviceId = field(fields, columns, DEVICE_ID);
        if (deviceId.isEmpty()) {
            throw new IllegalArgumentException("DeviceId is empty");
        }
        Instant measureTime = Instant.parse(field(fields, columns, MEASURE_TIME));
        Double temperatureCelsius = parseTemperature(
                field(fields, columns, TEMPERATURE), field(fields, columns, TEMP_UNIT));
        Double humidity = parseHumidity(field(fields, columns, HUMIDITY));

        return new SensorReading(deviceId, measureTime, temperatureCelsius, humidity);

    }

    private Double parseTemperature(String value, String unit) {
        if (value.isEmpty()) {
            return null;
        }
        if (unit.isEmpty()) {
            throw new IllegalArgumentException("Temperature '" + value + "' has no unit");
        }
        return TemperatureUnit.fromSymbol(unit).toCelsius(parseNumber(value, TEMPERATURE));
    }

    private Double parseHumidity(String value) {
        if (value.isEmpty()) {
            return null;
        }
        double humidity = parseNumber(value, HUMIDITY);
        if (humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("Humidity out of range [0, 100]: " + value);
        }
        return humidity;
    }

    private double parseNumber(String value, String column) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " is not a number: '" + value + "'");
        }
    }

    private String field(String[] fields, Map<String, Integer> columns, String column) {
        return fields[columns.get(column)].trim();
    }

    private Map<String, Integer> indexColumns(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        Map<String, Integer> columns = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            columns.put(headers[i].trim(), i);
        }

        return columns;
    }

}
