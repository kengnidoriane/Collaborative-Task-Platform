package com.collaborative.task.platform.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 * Supports both password and WebAuthn authentication.
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    String password,
    
    // WebAuthn fields
    String credentialId,
    String authenticatorData,
    String clientDataJSON,
    String signature,
    String userHandle
) {
    
    /**
     * Constructor for password-based login.
     */
    public LoginRequest(String email, String password) {
        this(email, password, null, null, null, null, null);
    }
    
    /**
     * Constructor for WebAuthn login.
     */
    public LoginRequest(String email, String credentialId, String authenticatorData, 
                       String clientDataJSON, String signature, String userHandle) {
        this(email, null, credentialId, authenticatorData, clientDataJSON, signature, userHandle);
    }
    
    /**
     * Check if this is a WebAuthn login request.
     */
    public boolean isWebAuthnLogin() {
        return credentialId != null && authenticatorData != null && 
               clientDataJSON != null && signature != null;
    }
    
    /**
     * Check if this is a password login request.
     */
    public boolean isPasswordLogin() {
        return password != null && !password.isBlank();
    }
}