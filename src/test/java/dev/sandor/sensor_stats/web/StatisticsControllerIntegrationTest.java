package dev.sandor.sensor_stats.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole application with the bundled readings.csv and calls the endpoint over MockMvc.
 */
@SpringBootTest(properties = "sensor.data.location=classpath:test-readings.csv")
@AutoConfigureMockMvc
class StatisticsControllerIntegrationTest {

    private static final String URL = "/api/v1/readings/statistics";

    @Autowired
    private MockMvcTester mvc;

    @Test
    void returnsLatestDayAveragesForAllDevicesByDefault() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "statistic": "avg",
                          "devices": [
                            { "deviceId": "1", "from": "2026-07-11T00:00:00Z", "to": "2026-07-12T00:00:00Z",
                              "readingCount": 1,
                              "metrics": { "temperature": { "value": 22.0, "sampleCount": 1 },
                                           "humidity":    { "value": 44.0, "sampleCount": 1 } } },
                            { "deviceId": "2", "from": "2026-07-10T00:00:00Z", "to": "2026-07-11T00:00:00Z",
                              "readingCount": 2,
                              "metrics": { "temperature": { "value": 25.83, "sampleCount": 2 },
                                           "humidity":    { "value": null, "sampleCount": 0 } } }
                          ]
                        }
                        """);
    }

    @Test
    void appliesAllQueryParameters() {
        assertThat(mvc.get().uri(URL)
                .param("deviceIds", "1")
                .param("from", "2026-07-10T00:00:00Z")
                .param("to", "2026-07-11T00:00:00Z")
                .param("metrics", "temperature")
                .param("statistic", "MAX"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "statistic": "max",
                          "devices": [
                            { "deviceId": "1", "readingCount": 3,
                              "metrics": { "temperature": { "value": 32.0, "sampleCount": 3 } } }
                          ]
                        }
                        """);
    }

    @Test
    void acceptsCommaSeparatedAndRepeatedDeviceIds() {
        assertThat(mvc.get().uri(URL).param("deviceIds", "1,2"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.devices[*].deviceId").asArray().containsExactly("1", "2");

        assertThat(mvc.get().uri(URL).param("deviceIds", "2").param("deviceIds", "1"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.devices[*].deviceId").asArray().containsExactly("1", "2");
    }

    @Test
    void rejectsUnknownStatisticAsProblemDetail() {
        assertThat(mvc.get().uri(URL).param("statistic", "median"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        { "status": 400, "title": "Invalid parameter" }
                        """);
    }

    @Test
    void rejectsUnknownMetric() {
        assertThat(mvc.get().uri(URL).param("metrics", "pressure"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").asString().contains("pressure");
    }

    @Test
    void rejectsMalformedDate() {
        assertThat(mvc.get().uri(URL).param("from", "yesterday"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInvertedRange() {
        assertThat(mvc.get().uri(URL)
                .param("from", "2026-07-11T00:00:00Z")
                .param("to", "2026-07-10T00:00:00Z"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Invalid time range");
    }

    @Test
    void returnsNotFoundWithUnknownDeviceIds() {
        assertThat(mvc.get().uri(URL).param("deviceIds", "1,42"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.deviceIds").asArray().containsExactly("42");
    }
}
