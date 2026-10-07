package dev.sandor.sensor_stats.web;

import dev.sandor.sensor_stats.query.Metric;
import dev.sandor.sensor_stats.query.ReadingStatisticsService;
import dev.sandor.sensor_stats.query.Statistic;
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

    @GetMapping("/statistics")
    public StatisticsResult getStatistics(
            @RequestParam(required = false) List<String> deviceIds,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) List<Metric> metrics,
            @RequestParam(required = false) Statistic statistic
    ) {
        return statisticsService.queryStatistics(deviceIds, from, to, metrics, statistic);
    }
}
