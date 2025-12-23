package com.collaborative.task.platform.dto.auth;

import com.collaborative.task.platform.entity.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for successful authentication.
 * Contains user information and JWT tokens.
 */
public record AuthResponse(
    UUID userId,
    String email,
    String fullName,
    String avatarUrl,
    Set<UserRole> roles,
    String accessToken,
    String refreshToken,
    LocalDateTime expiresAt,
    boolean hasWebAuthnCredentials
) {
    
    /**
     * Create response for successful authentication.
     */
    public static AuthResponse success(UUID userId, String email, String fullName, 
                                     String avatarUrl, Set<UserRole> roles, 
                                     String accessToken, String refreshToken, 
                                     LocalDateTime expiresAt, boolean hasWebAuthnCredentials) {
        return new AuthResponse(userId, email, fullName, avatarUrl, roles, 
                              accessToken, refreshToken, expiresAt, hasWebAuthnCredentials);
    }
}