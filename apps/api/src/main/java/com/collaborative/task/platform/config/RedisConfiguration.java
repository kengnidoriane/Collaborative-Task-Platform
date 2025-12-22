package com.collaborative.task.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis 7.2 configuration with JSON, Search, and Streams modules
 * Provides both reactive and traditional Redis access patterns
 */
@Configuration
public class RedisConfiguration {

    private final DatabaseProperties.RedisProperties redisProperties;

    public RedisConfiguration(DatabaseProperties databaseProperties) {
        this.redisProperties = databaseProperties.redis();
    }

    /**
     * Configure Redis connection factory with Lettuce for reactive operations
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            redisProperties.host(), 
            redisProperties.port()
        );
        
        if (redisProperties.password() != null && !redisProperties.password().isEmpty()) {
            factory.setPassword(redisProperties.password());
        }
        
        return factory;
    }

    /**
     * Configure reactive Redis template for async operations
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        org.springframework.data.redis.serializer.RedisSerializationContext<String, Object> serializationContext =
            org.springframework.data.redis.serializer.RedisSerializationContext
                .<String, Object>newSerializationContext(new StringRedisSerializer())
                .value(new GenericJackson2JsonRedisSerializer())
                .build();
        
        return new ReactiveRedisTemplate<String, Object>(
            connectionFactory,
            serializationContext
        );
    }

    /**
     * Configure traditional Redis template for synchronous operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * Configure Jedis pool for Redis modules (JSON, Search, Streams)
     */
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);
        
        if (redisProperties.password() != null && !redisProperties.password().isEmpty()) {
            return new JedisPool(poolConfig, redisProperties.host(), redisProperties.port(), 
                               2000, redisProperties.password());
        } else {
            return new JedisPool(poolConfig, redisProperties.host(), redisProperties.port());
        }
    }

    /**
     * Configure Redis Streams listener container for real-time events
     */
    @Bean
    public StreamMessageListenerContainer<String, ?> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ?> options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(java.time.Duration.ofMillis(100))
                .build();
                
        return StreamMessageListenerContainer.create(connectionFactory, options);
    }
}