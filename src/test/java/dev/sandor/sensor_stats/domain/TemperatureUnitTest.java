package dev.sandor.sensor_stats.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TemperatureUnitTest {

    @Test
    void convertsFahrenheitToCelsius() {
        assertThat(TemperatureUnit.FAHRENHEIT.toCelsius(32)).isEqualTo(0.0);
        assertThat(TemperatureUnit.FAHRENHEIT.toCelsius(212)).isEqualTo(100.0);
        assertThat(TemperatureUnit.FAHRENHEIT.toCelsius(77)).isCloseTo(25.0, within(1e-9));
    }

    @Test
    void keepsCelsiusUnchanged() {
        assertThat(TemperatureUnit.CELSIUS.toCelsius(23.5)).isEqualTo(23.5);
    }

    @Test
    void resolvesSymbolIgnoringCaseAndWhitespace() {
        assertThat(TemperatureUnit.fromSymbol("f")).isEqualTo(TemperatureUnit.FAHRENHEIT);
        assertThat(TemperatureUnit.fromSymbol(" C ")).isEqualTo(TemperatureUnit.CELSIUS);
    }

    @Test
    void rejectsUnknownSymbol() {
        assertThatThrownBy(() -> TemperatureUnit.fromSymbol("K"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("K");
    }
}
