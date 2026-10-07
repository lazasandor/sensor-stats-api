package dev.sandor.sensor_stats.data;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Loads the CSV once at startup. If the file cannot be read the application fails to start.
 */
@Configuration
@EnableConfigurationProperties(SensorDataProperties.class)
public class SensorDataLoader {

}
