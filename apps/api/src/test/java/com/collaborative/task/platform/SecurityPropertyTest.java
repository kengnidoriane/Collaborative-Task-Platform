package com.collaborative.task.platform;

import com.collaborative.task.platform.dto.auth.LoginRequest;
import com.collaborative.task.platform.service.JwtService;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for security functionality.
 * **Feature: collaborative-task-platform, Property 3: Authentication rejects invalid credentials**
 * **Validates: Requirements 1.3**
 */
public class SecurityPropertyTest {
    
    private static final JwtService jwtService = new JwtService(
        "testSecretKeyForTestingPurposesOnly123456789",
        900000L, // 15 minutes
        604800000L, // 7 days
        "collaborative-task-platform-test"
    );
    
    /**
     * Property 3: Authentication rejects invalid credentials
     * For any invalid JWT token, the system should reject it and throw appropriate exceptions.
     */
    @Property(tries = 100)
    void authenticationRejectsInvalidCredentials(
            @ForAll("invalidTokens") String invalidToken) {
        
        // Act & Assert - Invalid tokens should be rejected
        assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
            .isInstanceOf(RuntimeException.class);
    }
    
    /**
     * Property: Token extraction fails for invalid tokens
     * For any invalid token, user ID extraction should fail appropriately.
     */
    @Property(tries = 50)
    void tokenExtractionFailsForInvalidTokens(
            @ForAll("invalidTokens") String invalidToken) {
        
        // Act & Assert - Should not be able to extract user ID from invalid tokens
        assertThatThrownBy(() -> jwtService.getUserIdFromToken(invalidToken))
            .isInstanceOf(RuntimeException.class);
    }
    
    /**
     * Property: Invalid login requests are properly structured
     * For any invalid login data, the request should still be properly formed but identifiable as invalid.
     */
    @Property(tries = 50)
    void invalidLoginRequestsAreProperlyStructured(
            @ForAll("invalidEmails") String invalidEmail,
            @ForAll("invalidPasswords") String invalidPassword) {
        
        // Act
        LoginRequest request = new LoginRequest(invalidEmail, invalidPassword);
        
        // Assert - Request should be created but contain invalid data
        assertThat(request.email()).isEqualTo(invalidEmail);
        assertThat(request.password()).isEqualTo(invalidPassword);
        
        // Password login is only true if password is not null and not blank
        if (invalidPassword != null && !invalidPassword.isBlank()) {
            assertThat(request.isPasswordLogin()).isTrue();
        } else {
            assertThat(request.isPasswordLogin()).isFalse();
        }
        assertThat(request.isWebAuthnLogin()).isFalse();
    }
    
    /**
     * Test that validates security headers extraction from Authorization header.
     */
    @Test
    void authorizationHeaderExtractionSecurity() {
        // Test valid Bearer token
        String validHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        String extractedToken = jwtService.extractTokenFromHeader(validHeader);
        assertThat(extractedToken).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token");
        
        // Test invalid headers
        assertThat(jwtService.extractTokenFromHeader(null)).isNull();
        assertThat(jwtService.extractTokenFromHeader("")).isNull();
        assertThat(jwtService.extractTokenFromHeader("InvalidHeader")).isNull();
        assertThat(jwtService.extractTokenFromHeader("Basic dGVzdA==")).isNull();
    }
    
    /**
     * Test that validates token type checking security.
     */
    @Test
    void tokenTypeCheckingSecurity() {
        // Test with invalid tokens
        assertThat(jwtService.isAccessToken("invalid-token")).isFalse();
        assertThat(jwtService.isRefreshToken("invalid-token")).isFalse();
        assertThat(jwtService.isAccessToken("")).isFalse();
        assertThat(jwtService.isRefreshToken("")).isFalse();
    }
    
    // Generators for test data
    
    @Provide
    Arbitrary<String> invalidTokens() {
        return Arbitraries.oneOf(
                Arbitraries.just(""), // Empty token
                Arbitraries.just("invalid.token.here"), // Malformed token
                Arbitraries.just("not-a-jwt-token"), // Not JWT format
                Arbitraries.strings().ofMinLength(1).ofMaxLength(50), // Random strings
                Arbitraries.just("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.invalid.signature"), // Invalid signature
                Arbitraries.just("Bearer invalid-token"), // With Bearer prefix
                Arbitraries.just("null"), // Null string
                Arbitraries.just("undefined") // Undefined string
        );
    }
    
    @Provide
    Arbitrary<String> invalidEmails() {
        return Arbitraries.oneOf(
                Arbitraries.just(""), // Empty email
                Arbitraries.just("invalid-email"), // No @ symbol
                Arbitraries.just("@domain.com"), // No username
                Arbitraries.just("user@"), // No domain
                Arbitraries.just("user@.com"), // Invalid domain
                Arbitraries.just("user..name@domain.com"), // Double dots
                Arbitraries.strings().ofMinLength(1).ofMaxLength(10), // Random strings
                Arbitraries.just("user name@domain.com") // Spaces in email
        );
    }
    
    @Provide
    Arbitrary<String> invalidPasswords() {
        return Arbitraries.oneOf(
                Arbitraries.just(""), // Empty password
                Arbitraries.just("123"), // Too short
                Arbitraries.just("password"), // Common password
                Arbitraries.just("12345678"), // Only numbers
                Arbitraries.just("abcdefgh"), // Only lowercase
                Arbitraries.just("ABCDEFGH"), // Only uppercase
                Arbitraries.strings().ofMinLength(1).ofMaxLength(7) // Short random strings
        );
    }
}