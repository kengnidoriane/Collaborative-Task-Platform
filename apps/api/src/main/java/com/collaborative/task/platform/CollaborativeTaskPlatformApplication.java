package com.collaborative.task.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableR2dbcRepositories
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class CollaborativeTaskPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollaborativeTaskPlatformApplication.class, args);
    }
}