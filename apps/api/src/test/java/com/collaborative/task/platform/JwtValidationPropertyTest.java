package com.collaborative.task.platform;

import com.collaborative.task.platform.service.JwtService;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for JWT validation functionality.
 * **Feature: collaborative-task-platform, Property 5: JWT validation is comprehensive**
 * **Validates: Requirements 1.5**
 */
public class JwtValidationPropertyTest {
    
    private static final JwtService jwtService = new JwtService(
        "testSecretKeyForTestingPurposesOnly123456789",
        900000L, // 15 minutes
        604800000L, // 7 days
        "collaborative-task-platform-test"
    );
    
    /**
     * Property 5: JWT validation is comprehensive
     * For any invalid JWT token, validation should fail with appropriate error handling.
     */
    @Property(tries = 100)
    void jwtValidationIsComprehensive(
            @ForAll("invalidJwtTokens") String invalidToken) {
        
        // Act & Assert - Invalid JWT tokens should be rejected
        assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
            .isInstanceOf(RuntimeException.class);
        
        // Token type checking should also fail for invalid tokens
        assertThat(jwtService.isAccessToken(invalidToken)).isFalse();
        assertThat(jwtService.isRefreshToken(invalidToken)).isFalse();
        
        // Token expiration checking should handle invalid tokens
        assertThat(jwtService.isTokenExpired(invalidToken)).isTrue();
    }
    
    /**
     * Property: JWT header extraction is secure
     * For any authorization header, extraction should be secure and predictable.
     */
    @Property(tries = 50)
    void jwtHeaderExtractionIsSecure(
            @ForAll("authorizationHeaders") String authHeader) {
        
        // Act
        String extractedToken = jwtService.extractTokenFromHeader(authHeader);
        
        // Assert - Only valid Bearer tokens should be extracted
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            assertThat(extractedToken).isEqualTo(authHeader.substring(7));
        } else if (authHeader != null && authHeader.equals("Bearer ")) {
            assertThat(extractedToken).isEqualTo(""); // "Bearer " returns empty string
        } else {
            assertThat(extractedToken).isNull();
        }
    }
    
    /**
     * Test that validates JWT service configuration.
     */
    @Test
    void jwtServiceConfigurationIsValid() {
        // Test that JWT service is properly configured
        assertThat(jwtService).isNotNull();
        
        // Test token extraction with known values
        assertThat(jwtService.extractTokenFromHeader("Bearer test-token")).isEqualTo("test-token");
        assertThat(jwtService.extractTokenFromHeader("Bearer ")).isEqualTo("");
        assertThat(jwtService.extractTokenFromHeader("Bearer")).isNull();
        assertThat(jwtService.extractTokenFromHeader("Basic test")).isNull();
        assertThat(jwtService.extractTokenFromHeader(null)).isNull();
    }
    
    /**
     * Test that validates JWT token type checking.
     */
    @Test
    void jwtTokenTypeCheckingWorks() {
        // Test with various invalid tokens
        String[] invalidTokens = {
            "",
            "invalid",
            "not.a.jwt",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature",
            null
        };
        
        for (String token : invalidTokens) {
            if (token != null) {
                assertThat(jwtService.isAccessToken(token)).isFalse();
                assertThat(jwtService.isRefreshToken(token)).isFalse();
                assertThat(jwtService.isTokenExpired(token)).isTrue();
            }
        }
    }
    
    /**
     * Test that validates JWT validation error handling.
     */
    @Test
    void jwtValidationErrorHandlingWorks() {
        // Test various invalid token formats
        String[] invalidTokens = {
            "",
            "invalid-token",
            "not.jwt.format",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.invalid-signature"
        };
        
        for (String token : invalidTokens) {
            assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(RuntimeException.class);
                
            assertThatThrownBy(() -> jwtService.getUserIdFromToken(token))
                .isInstanceOf(RuntimeException.class);
                
            assertThatThrownBy(() -> jwtService.getEmailFromToken(token))
                .isInstanceOf(RuntimeException.class);
        }
    }
    
    // Generators for test data
    
    @Provide
    Arbitrary<String> invalidJwtTokens() {
        return Arbitraries.oneOf(
                Arbitraries.just(""), // Empty token
                Arbitraries.just("invalid"), // Not JWT format
                Arbitraries.just("not.jwt.format"), // Wrong number of parts
                Arbitraries.just("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature"), // Invalid signature
                Arbitraries.strings().ofMinLength(1).ofMaxLength(50), // Random strings
                Arbitraries.just("null"), // Null string
                Arbitraries.just("undefined"), // Undefined string
                Arbitraries.just("Bearer token"), // With Bearer prefix
                Arbitraries.just("eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJzdWIiOiIxMjM0NTY3ODkwIn0.") // None algorithm
        );
    }
    
    @Provide
    Arbitrary<String> authorizationHeaders() {
        return Arbitraries.oneOf(
                Arbitraries.just("Bearer valid-token-123"), // Valid Bearer token
                Arbitraries.just("Bearer "), // Bearer with empty token
                Arbitraries.just("Bearer"), // Bearer without space
                Arbitraries.just("Basic dGVzdA=="), // Basic auth
                Arbitraries.just(""), // Empty header
                Arbitraries.strings().ofMinLength(1).ofMaxLength(50), // Random strings
                Arbitraries.create(() -> null) // Null header
        );
    }
}