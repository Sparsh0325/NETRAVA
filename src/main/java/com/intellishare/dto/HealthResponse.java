package com.intellishare.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp,
        String environment
) {
    public static HealthResponse ok(String service, String version, String environment) {
        return new HealthResponse("UP", service, version, Instant.now(), environment);
    }
}
