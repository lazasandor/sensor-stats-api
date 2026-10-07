package dev.sandor.sensor_stats.query.exception;

import java.util.List;

public class UnknownDeviceException extends RuntimeException {

    private final List<String> deviceIds;
    public UnknownDeviceException(List<String> deviceIds) {
        super("Unknown device(s): " + String.join(", ", deviceIds));
        this.deviceIds = List.copyOf(deviceIds);
    }

    public List<String> getDeviceIds() {
        return deviceIds;
    }
}
