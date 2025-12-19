package com.collaborative.task.platform.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
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
     * Configure OpenTelemetry SDK with proper resource attributes
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, "collaborative-task-platform-api",
                ResourceAttributes.SERVICE_VERSION, "1.0.0",
                ResourceAttributes.DEPLOYMENT_ENVIRONMENT, 
                    environment.getActiveProfiles().length > 0 ? 
                        environment.getActiveProfiles()[0] : "development"
            )));

        SdkTracerProvider.Builder tracerProviderBuilder = SdkTracerProvider.builder()
            .setResource(resource);

        // Add Zipkin exporter for development
        if (environment.acceptsProfiles("development")) {
            tracerProviderBuilder.addSpanProcessor(
                BatchSpanProcessor.builder(
                    ZipkinSpanExporter.builder()
                        .setEndpoint("http://localhost:9411/api/v2/spans")
                        .build()
                ).build()
            );
        }

        // Add OTLP exporter for production
        if (environment.acceptsProfiles("production")) {
            String otlpEndpoint = environment.getProperty("otel.exporter.otlp.endpoint", 
                "http://localhost:4317");
            
            tracerProviderBuilder.addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint(otlpEndpoint)
                        .build()
                ).build()
            );
        }

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProviderBuilder.build())
            .setContextPropagators(ContextPropagators.create(
                W3CTraceContextPropagator.getInstance()
            ))
            .build();
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