package dev.sandor.sensor_stats.data;

import dev.sandor.sensor_stats.domain.SensorReading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CsvReadingParserTest {

    private static final String HEADER = "DeviceId,MeasureTime,Temperature,TempUnit,Humidity\n";

    private final CsvReadingParser parser = new CsvReadingParser();

    @Test
    void parsesEachValidRowExactlyOnce() {
        List<SensorReading> readings = parse(HEADER + "1,2026-07-10T10:00:00Z,23,C,42\n");

        assertThat(readings).containsExactly(
                new SensorReading("1", Instant.parse("2026-07-10T10:00:00Z"), 23.0, 42.0));
    }

    @Test
    void convertsFahrenheitToCelsius() {
        List<SensorReading> readings = parse(HEADER + "2,2026-07-10T10:00:00Z,77,F,30\n");

        assertThat(readings.getFirst().temperatureCelsius()).isCloseTo(25.0, within(1e-9));
    }

    @Test
    void keepsMissingTrailingHumidityAsNull() {
        List<SensorReading> readings = parse(HEADER + "2,2026-07-10T10:00:00Z,77,F,\n");

        assertThat(readings).hasSize(1);
        assertThat(readings.getFirst().humidity()).isNull();
    }

    @Test
    void keepsMissingTemperatureAsNull() {
        List<SensorReading> readings = parse(HEADER + "1,2026-07-10T10:00:00Z,,,42\n");

        assertThat(readings.getFirst().temperatureCelsius()).isNull();
        assertThat(readings.getFirst().humidity()).isEqualTo(42.0);
    }

    @Test
    void findsColumnsByHeaderNameNotByPosition() {
        List<SensorReading> readings = parse("""
                Humidity,TempUnit,Temperature,MeasureTime,DeviceId
                42,C,23,2026-07-10T10:00:00Z,1
                """);

        assertThat(readings).containsExactly(
                new SensorReading("1", Instant.parse("2026-07-10T10:00:00Z"), 23.0, 42.0));
    }

    @Test
    void skipsBlankLines() {
        List<SensorReading> readings = parse(HEADER
                + "1,2026-07-10T10:00:00Z,23,C,42\n"
                + "\n"
                + "   \n"
                + "1,2026-07-10T14:00:00Z,32,C,45\n");

        assertThat(readings).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1,not-a-date,23,C,42",            // malformed timestamp
            "1,2026-07-10T10:00:00Z,23,K,42",  // unknown unit
            "1,2026-07-10T10:00:00Z,abc,C,42", // temperature is not a number
            "1,2026-07-10T10:00:00Z,23,,42",   // temperature without unit
            "1,2026-07-10T10:00:00Z,23,C,140", // humidity out of range
            "1,2026-07-10T10:00:00Z,23,C,-1",  // humidity out of range
            ",2026-07-10T10:00:00Z,23,C,42",   // empty device id
            "1,2026-07-10T10:00:00Z"           // too few fields
    })
    void rejectsInvalidRowWithItsLineNumber(String invalidRow) {
        String csv = HEADER + "1,2026-07-10T09:00:00Z,20,C,40\n" + invalidRow + "\n";

        assertThatThrownBy(() -> parse(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 3");
    }

    @Test
    void rejectsHeaderWithMissingColumns() {
        assertThatThrownBy(() -> parse("DeviceId,MeasureTime,Temperature\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TempUnit")
                .hasMessageContaining("Humidity");
    }

    @Test
    void rejectsEmptyInput() {
        assertThatThrownBy(() -> parse(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private List<SensorReading> parse(String csv) {
        return parser.parse(new BufferedReader(new StringReader(csv)));
    }
}
