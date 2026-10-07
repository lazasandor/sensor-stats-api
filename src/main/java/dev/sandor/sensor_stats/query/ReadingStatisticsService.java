package dev.sandor.sensor_stats.query;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ReadingStatisticsService {

    public StatisticsResult queryStatistics(String deviceId,
                                            Instant from,
                                            Instant to,
                                            List<Metric> metrics,
                                            Statistic statistic) {
        //TODO
        return new StatisticsResult(statistic.key(), List.of());
    }
}
