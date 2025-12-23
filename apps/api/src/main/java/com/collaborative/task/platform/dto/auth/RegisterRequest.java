package com.collaborative.task.platform.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 * Implements clean validation with comprehensive constraints.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    String password,
    
    @NotBlank(message = "Full name is required")
    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    String fullName,
    
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    String avatarUrl
) {
    
    /**
     * Constructor with optional avatar URL.
     */
    public RegisterRequest(String email, String password, String fullName) {
        this(email, password, fullName, null);
    }
}