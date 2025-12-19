package com.collaborative.task.platform.service;

import com.collaborative.task.platform.config.ApplicationProperties;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * System information service demonstrating SOLID principles
 * Single Responsibility: Provides system information
 * Dependency Inversion: Depends on abstractions (ApplicationProperties)
 */
@Service
@Observed(name = "system-info-service")
public class SystemInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoService.class);
    
    private final ApplicationProperties applicationProperties;

    public SystemInfoService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Get comprehensive system information
     * Demonstrates clean code principles with clear method naming and single responsibility
     */
    public Map<String, Object> getSystemInfo() {
        logger.debug("Retrieving system information");
        
        return Map.of(
            "application", getApplicationInfo(),
            "runtime", getRuntimeInfo(),
            "configuration", getConfigurationInfo(),
            "timestamp", Instant.now()
        );
    }

    private Map<String, Object> getApplicationInfo() {
        return Map.of(
            "name", "Collaborative Task Platform API",
            "version", "1.0.0",
            "description", "Real-time collaborative task management platform"
        );
    }

    private Map<String, Object> getRuntimeInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        return Map.of(
            "javaVersion", System.getProperty("java.version"),
            "virtualThreadsEnabled", Thread.currentThread().isVirtual(),
            "availableProcessors", runtime.availableProcessors(),
            "maxMemory", runtime.maxMemory(),
            "totalMemory", runtime.totalMemory(),
            "freeMemory", runtime.freeMemory()
        );
    }

    private Map<String, Object> getConfigurationInfo() {
        return Map.of(
            "websocketPort", applicationProperties.websocket().port(),
            "jwtExpiration", applicationProperties.jwt().expiration(),
            "corsEnabled", applicationProperties.cors().allowCredentials()
        );
    }
}