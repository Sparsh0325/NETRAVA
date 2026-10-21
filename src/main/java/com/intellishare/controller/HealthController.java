package com.intellishare.controller;

import com.intellishare.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final String serviceName;
    private final String version;
    private final Environment environment;

    public HealthController(
            @Value("${spring.application.name:intellishare-backend}") String serviceName,
            @Value("${intellishare.version:0.1.0}") String version,
            Environment environment
    ) {
        this.serviceName = serviceName;
        this.version = version;
        this.environment = environment;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeEnv = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";
        return ResponseEntity.ok(HealthResponse.ok(serviceName, version, activeEnv));
    }
}
