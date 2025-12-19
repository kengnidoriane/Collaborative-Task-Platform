package com.collaborative.task.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration properties using Java records for immutability
 * and clean configuration management following SOLID principles
 */
@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
    JwtProperties jwt,
    CorsProperties cors,
    WebSocketProperties websocket
) {
    
    public record JwtProperties(
        String secret,
        long expiration,
        long refreshExpiration
    ) {}
    
    public record CorsProperties(
        String allowedOrigins,
        String allowedMethods,
        String allowedHeaders,
        boolean allowCredentials
    ) {}
    
    public record WebSocketProperties(
        String allowedOrigins,
        int port
    ) {}
}