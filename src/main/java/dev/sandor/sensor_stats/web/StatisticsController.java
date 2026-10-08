package dev.sandor.sensor_stats.web;

import dev.sandor.sensor_stats.query.Metric;
import dev.sandor.sensor_stats.query.ReadingStatisticsService;
import dev.sandor.sensor_stats.query.Statistic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static dev.sandor.sensor_stats.query.ReadingStatisticsService.StatisticsResult;

@RestController
@RequestMapping("/api/v1/readings")
public class StatisticsController {

    private final ReadingStatisticsService statisticsService;

    public StatisticsController(ReadingStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(
            summary = "Aggregated statistics of sensor readings, per device"
    )
    @ApiResponse(responseCode = "200", description = "Statistics per device")
    @ApiResponse(responseCode = "400", description = "Invalid parameter value or time range")
    @ApiResponse(responseCode = "404", description = "One or more devices do not exist")
    @GetMapping("/statistics")
    public StatisticsResult getStatistics(
            @Parameter(description = "Device IDs (default is all).", example = "1,2")
            @RequestParam(required = false) List<String> deviceIds,

            @Parameter(description = "Start of the range, inclusive.", example = "2026-07-10T00:00:00Z")
            @RequestParam(required = false) Instant from,

            @Parameter(description = "End of the range, exclusive.", example = "2026-07-11T00:00:00Z")
            @RequestParam(required = false) Instant to,

            @Parameter(description = "Metrics to include (default is all).",
                    array = @ArraySchema(schema = @Schema(allowableValues = {"temperature", "humidity"})))
            @RequestParam(required = false) List<Metric> metrics,

            @Parameter(description = "Statistic to compute (default is avg).",
                    schema = @Schema(allowableValues = {"min", "max", "avg"}, defaultValue = "avg"))
            @RequestParam(required = false) Statistic statistic
    ) {
        return statisticsService.queryStatistics(
                deviceIds, from, to, metrics, statistic
        );
    }
}
