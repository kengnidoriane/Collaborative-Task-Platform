package com.collaborative.task.platform.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * OpenTelemetry and observability configuration
 * Provides distributed tracing, metrics, and monitoring capabilities
 */
@Configuration
public class ObservabilityConfiguration {

    private final Environment environment;

    public ObservabilityConfiguration(Environment environment) {
        this.environment = environment;
    }

    /**
     * Enable @Observed annotation support
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    /**
     * Configure meter registry for metrics collection
     */
    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}