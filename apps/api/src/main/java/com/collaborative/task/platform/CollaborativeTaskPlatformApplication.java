package com.collaborative.task.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;

@SpringBootApplication
@EnableJpaAuditing
@EnableR2dbcRepositories
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@ConfigurationPropertiesScan
public class CollaborativeTaskPlatformApplication {

    public static void main(String[] args) {
        // Enable Virtual Threads for the application
        System.setProperty("spring.threads.virtual.enabled", "true");
        SpringApplication.run(CollaborativeTaskPlatformApplication.class, args);
    }

    /**
     * Configure Tomcat to use Virtual Threads for handling requests
     * This enables ultra-fast request processing with Java 21 Virtual Threads
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}