package com.collaborative.task.platform.service;

import com.collaborative.task.platform.dto.auth.AuthResponse;
import com.collaborative.task.platform.dto.auth.LoginRequest;
import com.collaborative.task.platform.dto.auth.RegisterRequest;
import com.collaborative.task.platform.entity.User;
import com.collaborative.task.platform.entity.UserRole;
import com.collaborative.task.platform.exception.AuthenticationException;
import com.collaborative.task.platform.exception.BusinessException;
import com.collaborative.task.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Main authentication service with comprehensive security features.
 * Handles user registration, login, and session management.
 */
@Service
@Transactional
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final WebAuthnService webAuthnService;
    private final SessionService sessionService;
    
    public AuthenticationService(
            UserRepository userRepository,
            PasswordService passwordService,
            JwtService jwtService,
            WebAuthnService webAuthnService,
            SessionService sessionService) {
        
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.webAuthnService = webAuthnService;
        this.sessionService = sessionService;
    }
    
    /**
     * Register a new user with secure password hashing.
     * 
     * @param request the registration request
     * @return authentication response with tokens
     */
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting user registration for email: {}", request.email());
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered", HttpStatus.CONFLICT, "EMAIL_EXISTS");
        }
        
        // Validate password strength
        if (!passwordService.isPasswordStrong(request.password())) {
            throw new BusinessException("Password does not meet strength requirements", HttpStatus.BAD_REQUEST, "WEAK_PASSWORD");
        }
        
        // Create new user
        User user = new User(request.email(), request.fullName());
        user.setPasswordHash(passwordService.hashPassword(request.password()));
        user.setAvatarUrl(request.avatarUrl());
        user.setRoles(Set.of(UserRole.USER));
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);
        
        // Create session
        sessionService.createSession(savedUser.getId(), accessToken, refreshToken);
        
        logger.info("User registered successfully: {}", savedUser.getEmail());
        
        return AuthResponse.success(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getAvatarUrl(),
                savedUser.getRoles(),
                accessToken,
                refreshToken,
                jwtService.getExpirationTime(accessToken),
                webAuthnService.hasCredentials(savedUser.getId())
        );
    }
    
    /**
     * Authenticate user with password or WebAuthn.
     * 
     * @param request the login request
     * @return authentication response with tokens
     */
    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting login for email: {}", request.email());
        
        // Find user by email
        Optional<User> userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid credentials");
        }
        
        User user = userOpt.get();
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new AuthenticationException("Account is locked due to too many failed attempts");
        }
        
        // Check if email is verified
        if (!user.isEmailVerified()) {
            throw new AuthenticationException("Email not verified");
        }
        
        // Authenticate based on request type
        if (request.isPasswordLogin()) {
            return authenticateWithPassword(user, request.password());
        } else if (request.isWebAuthnLogin()) {
            return authenticateWithWebAuthn(user, request);
        } else {
            throw new AuthenticationException("Invalid authentication method");
        }
    }
    
    /**
     * Authenticate user with password.
     */
    private AuthResponse authenticateWithPassword(User user, String password) {
        if (user.getPasswordHash() == null) {
            throw new AuthenticationException("Password authentication not available for this account");
        }
        
        if (!passwordService.verifyPassword(password, user.getPasswordHash())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            
            logger.warn("Failed login attempt for user: {}", user.getEmail());
            throw new AuthenticationException("Invalid credentials");
        }
        
        return completeSuccessfulLogin(user);
    }
    
    /**
     * Authenticate user with WebAuthn.
     */
    private AuthResponse authenticateWithWebAuthn(User user, LoginRequest request) {
        try {
            // This is a simplified implementation
            // In a real implementation, you would validate the WebAuthn assertion
            if (!webAuthnService.hasCredentials(user.getId())) {
                throw new AuthenticationException("WebAuthn credentials not found");
            }
            
            return completeSuccessfulLogin(user);
        } catch (Exception e) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            
            logger.warn("Failed WebAuthn login attempt for user: {}", user.getEmail(), e);
            throw new AuthenticationException("WebAuthn authentication failed");
        }
    }
    
    /**
     * Complete successful login process.
     */
    private AuthResponse completeSuccessfulLogin(User user) {
        // Reset failed attempts
        user.resetFailedLoginAttempts();
        user.setLastLogin(LocalDateTime.now());
        
        // Check if password needs rehashing
        if (user.getPasswordHash() != null && passwordService.needsRehash(user.getPasswordHash())) {
            logger.info("Password hash needs updating for user: {}", user.getEmail());
            // Note: Password rehashing would happen on next password change
        }
        
        User updatedUser = userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(updatedUser);
        String refreshToken = jwtService.generateRefreshToken(updatedUser);
        
        // Create session
        sessionService.createSession(updatedUser.getId(), accessToken, refreshToken);
        
        logger.info("User logged in successfully: {}", updatedUser.getEmail());
        
        return AuthResponse.success(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getFullName(),
                updatedUser.getAvatarUrl(),
                updatedUser.getRoles(),
                accessToken,
                refreshToken,
                jwtService.getExpirationTime(accessToken),
                webAuthnService.hasCredentials(updatedUser.getId())
        );
    }
    
    /**
     * Refresh access token using refresh token.
     * 
     * @param refreshToken the refresh token
     * @return new authentication response
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new AuthenticationException("Invalid refresh token");
            }
            
            UUID userId = jwtService.getUserIdFromToken(refreshToken);
            
            // Check if session exists
            if (!sessionService.isValidSession(userId, refreshToken)) {
                throw new AuthenticationException("Invalid session");
            }
            
            // Get user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                throw new AuthenticationException("User not found or inactive");
            }
            
            User user = userOpt.get();
            
            // Generate new tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            
            // Update session
            sessionService.updateSession(userId, newAccessToken, newRefreshToken);
            
            logger.debug("Token refreshed for user: {}", user.getEmail());
            
            return AuthResponse.success(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getAvatarUrl(),
                    user.getRoles(),
                    newAccessToken,
                    newRefreshToken,
                    jwtService.getExpirationTime(newAccessToken),
                    webAuthnService.hasCredentials(user.getId())
            );
            
        } catch (Exception e) {
            logger.warn("Token refresh failed", e);
            throw new AuthenticationException("Token refresh failed");
        }
    }
    
    /**
     * Logout user and invalidate session.
     * 
     * @param userId the user ID
     * @param accessToken the access token
     */
    public void logout(UUID userId, String accessToken) {
        sessionService.invalidateSession(userId, accessToken);
        logger.info("User logged out: {}", userId);
    }
    
    /**
     * Validate access token and get user.
     * 
     * @param accessToken the access token
     * @return user if valid
     */
    public Optional<User> validateToken(String accessToken) {
        try {
            if (!jwtService.isAccessToken(accessToken)) {
                return Optional.empty();
            }
            
            UUID userId = jwtService.getUserIdFromToken(accessToken);
            
            // Check if session is valid
            if (!sessionService.isValidSession(userId, accessToken)) {
                return Optional.empty();
            }
            
            return userRepository.findById(userId)
                    .filter(User::isActive);
                    
        } catch (Exception e) {
            logger.debug("Token validation failed", e);
            return Optional.empty();
        }
    }
}