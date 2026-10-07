package dev.sandor.sensor_stats.web;

import dev.sandor.sensor_stats.query.Metric;
import dev.sandor.sensor_stats.query.Statistic;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets clients use lower-case values instead of the enum names in query parameters.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(org.springframework.format.FormatterRegistry registry) {
        registry.addConverter(String.class, Metric.class, Metric::fromKey);
        registry.addConverter(String.class, Statistic.class, Statistic::fromKey);
    }
}
