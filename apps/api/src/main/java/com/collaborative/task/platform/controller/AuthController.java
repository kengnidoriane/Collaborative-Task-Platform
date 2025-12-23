package com.collaborative.task.platform.controller;

import com.collaborative.task.platform.dto.auth.AuthResponse;
import com.collaborative.task.platform.dto.auth.LoginRequest;
import com.collaborative.task.platform.dto.auth.RegisterRequest;
import com.collaborative.task.platform.service.AuthenticationService;
import com.collaborative.task.platform.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for authentication endpoints.
 * Provides secure user registration, login, and token management.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    
    public AuthController(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }
    
    /**
     * Register a new user.
     * Rate limited to 5 requests per minute per IP.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        logger.info("Registration attempt from IP: {} for email: {}", clientIp, request.email());
        
        try {
            AuthResponse response = authenticationService.register(request);
            
            logger.info("User registered successfully: {}", request.email());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.warn("Registration failed for email: {} from IP: {}", request.email(), clientIp, e);
            throw e;
        }
    }
    
    /**
     * Authenticate user (login).
     * Rate limited to 10 requests per minute per IP.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        logger.info("Login attempt from IP: {} for email: {}", clientIp, request.email());
        
        try {
            AuthResponse response = authenticationService.login(request);
            
            logger.info("User logged in successfully: {}", request.email());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.warn("Login failed for email: {} from IP: {}", request.email(), clientIp, e);
            throw e;
        }
    }
    
    /**
     * Refresh access token using refresh token.
     * Rate limited to 30 requests per minute per user.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        String clientIp = getClientIpAddress(httpRequest);
        logger.debug("Token refresh attempt from IP: {}", clientIp);
        
        try {
            AuthResponse response = authenticationService.refreshToken(refreshToken);
            
            logger.debug("Token refreshed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.warn("Token refresh failed from IP: {}", clientIp, e);
            throw e;
        }
    }
    
    /**
     * Logout user and invalidate session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest) {
        
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = jwtService.extractTokenFromHeader(authHeader);
        
        if (accessToken != null) {
            try {
                UUID userId = jwtService.getUserIdFromToken(accessToken);
                authenticationService.logout(userId, accessToken);
                
                logger.info("User logged out successfully: {}", userId);
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
                
            } catch (Exception e) {
                logger.warn("Logout failed", e);
                // Still return success to prevent information leakage
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }
        }
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    /**
     * Validate current token and get user info.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            HttpServletRequest httpRequest) {
        
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = jwtService.extractTokenFromHeader(authHeader);
        
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            var userOpt = authenticationService.validateToken(accessToken);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                
                Map<String, Object> userInfo = Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                        "roles", user.getRoles(),
                        "emailVerified", user.isEmailVerified(),
                        "lastLogin", user.getLastLogin()
                );
                
                return ResponseEntity.ok(userInfo);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
        } catch (Exception e) {
            logger.warn("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    
    /**
     * Check if email is available for registration.
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(
            @RequestParam String email) {
        
        try {
            // This would use the user repository to check email existence
            // For now, we'll return a simple response
            boolean available = !email.equals("test@example.com"); // Placeholder logic
            
            return ResponseEntity.ok(Map.of("available", available));
            
        } catch (Exception e) {
            logger.error("Email availability check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get password strength score.
     */
    @PostMapping("/password-strength")
    public ResponseEntity<Map<String, Object>> checkPasswordStrength(
            @RequestBody Map<String, String> request) {
        
        String password = request.get("password");
        if (password == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // This would use the password service
            int strength = 75; // Placeholder
            boolean isStrong = strength >= 70;
            
            Map<String, Object> result = Map.of(
                    "strength", strength,
                    "isStrong", isStrong,
                    "feedback", isStrong ? "Strong password" : "Password could be stronger"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Password strength check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Extract client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}