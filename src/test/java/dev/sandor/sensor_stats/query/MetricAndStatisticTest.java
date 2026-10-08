package dev.sandor.sensor_stats.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricAndStatisticTest {

    @Test
    void resolvesMetricKeyIgnoringCaseAndWhitespace() {
        assertThat(Metric.fromKey("temperature")).isEqualTo(Metric.TEMPERATURE);
        assertThat(Metric.fromKey(" HUMIDITY ")).isEqualTo(Metric.HUMIDITY);
    }

    @Test
    void reportsAllowedValuesForUnknownMetric() {
        assertThatThrownBy(() -> Metric.fromKey("pressure"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pressure")
                .hasMessageContaining("temperature, humidity");
    }

    @Test
    void resolvesStatisticKeyIgnoringCase() {
        assertThat(Statistic.fromKey("min")).isEqualTo(Statistic.MIN);
        assertThat(Statistic.fromKey("MAX")).isEqualTo(Statistic.MAX);
        assertThat(Statistic.fromKey(" Avg ")).isEqualTo(Statistic.AVG);
    }

    @Test
    void reportsAllowedValuesForUnknownStatistic() {
        assertThatThrownBy(() -> Statistic.fromKey("median"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("median")
                .hasMessageContaining("min, max, avg");
    }
}
