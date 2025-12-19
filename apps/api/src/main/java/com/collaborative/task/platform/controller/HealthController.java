package com.collaborative.task.platform.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller to verify backend foundation components
 * Tests connectivity to PostgreSQL, MongoDB, and Redis
 */
@RestController
@RequestMapping("/health")
public class HealthController implements HealthIndicator {

    private final DatabaseClient databaseClient;
    private final ReactiveMongoTemplate mongoTemplate;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public HealthController(
            DatabaseClient databaseClient,
            ReactiveMongoTemplate mongoTemplate,
            ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.databaseClient = databaseClient;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Test database connections
            boolean postgresHealthy = testPostgresConnection();
            boolean mongoHealthy = testMongoConnection();
            boolean redisHealthy = testRedisConnection();

            if (postgresHealthy && mongoHealthy && redisHealthy) {
                return Health.up()
                    .withDetail("postgres", "UP")
                    .withDetail("mongodb", "UP")
                    .withDetail("redis", "UP")
                    .withDetail("timestamp", Instant.now())
                    .build();
            } else {
                return Health.down()
                    .withDetail("postgres", postgresHealthy ? "UP" : "DOWN")
                    .withDetail("mongodb", mongoHealthy ? "UP" : "DOWN")
                    .withDetail("redis", redisHealthy ? "UP" : "DOWN")
                    .withDetail("timestamp", Instant.now())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", Instant.now())
                .build();
        }
    }

    @GetMapping("/detailed")
    public Mono<ResponseEntity<Map<String, Object>>> detailedHealth() {
        return Mono.fromCallable(() -> {
            Health health = health();
            return ResponseEntity.ok(Map.of(
                "status", health.getStatus().getCode(),
                "details", health.getDetails(),
                "components", Map.of(
                    "virtualThreads", Thread.currentThread().isVirtual(),
                    "javaVersion", System.getProperty("java.version"),
                    "springProfile", System.getProperty("spring.profiles.active", "default")
                )
            ));
        });
    }

    private boolean testPostgresConnection() {
        try {
            return databaseClient.sql("SELECT 1")
                .fetch()
                .rowsUpdated()
                .block() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testMongoConnection() {
        try {
            return mongoTemplate.getCollectionNames()
                .hasElements()
                .block() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testRedisConnection() {
        try {
            return redisTemplate.opsForValue()
                .set("health-check", "ok")
                .then(redisTemplate.delete("health-check"))
                .block() != null;
        } catch (Exception e) {
            return false;
        }
    }
}