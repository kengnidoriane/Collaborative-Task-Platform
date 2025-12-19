package com.collaborative.task.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Database configuration properties using records for immutability
 */
@ConfigurationProperties(prefix = "spring.data")
public record DatabaseProperties(
    MongoDbProperties mongodb,
    RedisProperties redis
) {
    public record MongoDbProperties(
        String uri,
        String database
    ) {}
    
    public record RedisProperties(
        String host,
        int port,
        String password,
        String timeout
    ) {}
}