package com.collaborative.task.platform;

import com.collaborative.task.platform.config.ApplicationProperties;
import com.collaborative.task.platform.service.SystemInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify backend foundation components are properly configured
 * Tests SOLID principles implementation and dependency injection
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.jwt.secret=test-secret",
    "app.jwt.expiration=86400000",
    "app.jwt.refresh-expiration=604800000",
    "app.cors.allowed-origins=http://localhost:3000",
    "app.cors.allowed-methods=GET,POST,PUT,DELETE",
    "app.cors.allowed-headers=*",
    "app.cors.allow-credentials=true",
    "app.websocket.allowed-origins=http://localhost:3000",
    "app.websocket.port=8081",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test",
    "spring.data.mongodb.database=test",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.password=",
    "spring.data.redis.timeout=2000ms"
})
class BackendFoundationTest {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully with all configurations
        assertTrue(true, "Spring context should load without errors");
    }

    @Test
    void testApplicationPropertiesRecord() {
        // Test that configuration properties records work correctly
        ApplicationProperties.JwtProperties jwt = new ApplicationProperties.JwtProperties(
            "test-secret", 86400000L, 604800000L);
        
        assertEquals("test-secret", jwt.secret());
        assertEquals(86400000L, jwt.expiration());
        assertEquals(604800000L, jwt.refreshExpiration());
    }

    @Test
    void testSystemInfoServiceDependencyInjection() {
        // Test that dependency injection works with records
        ApplicationProperties.JwtProperties jwt = new ApplicationProperties.JwtProperties(
            "test-secret", 86400000L, 604800000L);
        ApplicationProperties.CorsProperties cors = new ApplicationProperties.CorsProperties(
            "http://localhost:3000", "GET,POST", "*", true);
        ApplicationProperties.WebSocketProperties websocket = new ApplicationProperties.WebSocketProperties(
            "http://localhost:3000", 8081);
        
        ApplicationProperties props = new ApplicationProperties(jwt, cors, websocket);
        SystemInfoService service = new SystemInfoService(props);
        
        var systemInfo = service.getSystemInfo();
        
        assertNotNull(systemInfo);
        assertTrue(systemInfo.containsKey("application"));
        assertTrue(systemInfo.containsKey("runtime"));
        assertTrue(systemInfo.containsKey("configuration"));
        assertTrue(systemInfo.containsKey("timestamp"));
    }

    @Test
    void testVirtualThreadsEnabled() {
        // Test that Virtual Threads are properly configured
        String virtualThreadsProperty = System.getProperty("spring.threads.virtual.enabled");
        assertEquals("true", virtualThreadsProperty, "Virtual Threads should be enabled");
    }
}