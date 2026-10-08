# Sensor statistics API

A Spring Boot REST API that loads IoT sensor readings from a CSV file and returns
aggregated statistics (min / max / average) of temperature and humidity, per device.

## Tech stack

Java 21, Spring Boot 4.1 (Spring MVC), Maven, springdoc-openapi (Swagger UI),
JUnit 5, AssertJ, MockMvc.

## Running the application

Requirements: Java 21. No local Maven installation is needed, the Maven wrapper is included.

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`.

The API is available at `http://localhost:8080`, the interactive API documentation
at `http://localhost:8080/swagger-ui.html`.

By default the bundled `src/main/resources/readings.csv` is loaded. Another file can be used
without rebuilding:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--sensor.data.location=file:/path/to/readings.csv
```

## Running the tests

```bash
./mvnw test
```

49 tests: unit tests for the CSV parsing, the domain and query model and the statistics
service, plus integration tests that call the endpoint through the full application.

## API

### `GET /api/v1/readings/statistics`

All parameters are optional.

| Parameter   | Default                       | Description                                                       |
|-------------|-------------------------------|-------------------------------------------------------------------|
| `deviceIds` | all devices                   | Comma separated (`deviceIds=1,2`) or repeated (`deviceIds=1&deviceIds=2`) |
| `from`      | latest day, see below         | Start of the range, inclusive, ISO-8601 UTC (`2026-07-10T00:00:00Z`) |
| `to`        | latest day, see below         | End of the range, exclusive, ISO-8601 UTC                        |
| `metrics`   | all metrics                   | `temperature`, `humidity`                                         |
| `statistic` | `avg`                         | `min`, `max`, `avg`                                               |

Parameter values are case-insensitive. If neither `from` nor `to` is given, the latest
available data is used (see [Design decisions](#design-decisions)).

### Examples

Latest data of all devices, all metrics, average:

```bash
curl "http://localhost:8080/api/v1/readings/statistics"
```

Maximum temperature of device 1 on 10 July 2026:

```bash
curl "http://localhost:8080/api/v1/readings/statistics?deviceIds=1&metrics=temperature&statistic=max&from=2026-07-10T00:00:00Z&to=2026-07-11T00:00:00Z"
```

```json
{
  "statistic": "max",
  "devices": [
    {
      "deviceId": "1",
      "from": "2026-07-10T00:00:00Z",
      "to": "2026-07-11T00:00:00Z",
      "readingCount": 3,
      "metrics": {
        "temperature": { "value": 32.0, "unit": "°C", "sampleCount": 3 }
      }
    }
  ]
}
```

Response fields:

- `from` / `to`: the time window that was actually used for the device.
- `readingCount`: number of readings of the device in the window.
- `value`: the statistic, rounded to 2 decimals; `null` if no reading in the window reported the metric.
- `sampleCount`: number of readings that reported the metric, e.g. `readingCount: 2` with
  `sampleCount: 0` means the device sent readings but no humidity.

### Errors

Errors are returned as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details
(`application/problem+json`).

| Status | When                                                                    |
|--------|-------------------------------------------------------------------------|
| 400    | Unknown metric or statistic, malformed date, `from` not before `to`     |
| 404    | One or more requested devices do not exist (listed in `deviceIds`)      |

```json
{
  "title": "Unknown device(s)",
  "status": 404,
  "detail": "Unknown device(s): 42",
  "instance": "/api/v1/readings/statistics",
  "deviceIds": ["42"]
}
```

## Project structure

```
src/main/java/dev/sandor/sensor_stats
├── domain/   SensorReading, TemperatureUnit         - the data and unit conversion
├── data/     CsvReadingParser, SensorDataLoader,
│             SensorDataProperties                   - loading and storing the readings
├── query/    ReadingStatisticsService, Metric,
│             Statistic, TimeWindow, exception/      - query logic
└── web/      StatisticsController, WebConfig,
              ApiExceptionHandler                    - REST layer and error handling
```

The integration tests use their own fixture file (`src/test/resources/test-readings.csv`),
so changing the production data does not break them.
