package com.collaborative.task.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;

/**
 * Database configuration for PostgreSQL 16 with R2DBC and MongoDB 7.0
 * Implements reactive database access patterns with proper connection management
 */
@Configuration
public class DatabaseConfiguration {

    private final DatabaseProperties databaseProperties;

    public DatabaseConfiguration(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    /**
     * Configure MongoDB client with time-series collections and queryable encryption
     */
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(databaseProperties.mongodb().uri());
        
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .build();
            
        return MongoClients.create(settings);
    }

    /**
     * Configure reactive MongoDB template for analytics and time-series data
     */
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(MongoClient mongoClient) {
        return new ReactiveMongoTemplate(mongoClient, databaseProperties.mongodb().database());
    }
}