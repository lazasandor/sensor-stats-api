package dev.sandor.sensor_stats.data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

/**
 * Where the readings are loaded from. Can be overridden int the properties file.
 */
@ConfigurationProperties("sensor.data")
public record SensorDataProperties(@DefaultValue("classpath:readings.csv") Resource location) {
}
