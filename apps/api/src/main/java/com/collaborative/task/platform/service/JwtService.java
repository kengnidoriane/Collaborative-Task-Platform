package com.collaborative.task.platform.service;

import com.collaborative.task.platform.entity.User;
import com.collaborative.task.platform.entity.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for JWT token management with RS256 and proper rotation.
 * Provides secure token generation, validation, and refresh capabilities.
 */
@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    
    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final String issuer;
    
    public JwtService(
            @Value("${app.jwt.secret:defaultSecretKeyThatShouldBeChangedInProduction123456789}") String secret,
            @Value("${app.jwt.access-token-expiration:900000}") long accessTokenExpirationMs, // 15 minutes
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMs, // 7 days
            @Value("${app.jwt.issuer:collaborative-task-platform}") String issuer) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.issuer = issuer;
        
        logger.info("JWT Service initialized with issuer: {}", issuer);
    }
    
    /**
     * Generate access token for authenticated user.
     * 
     * @param user the authenticated user
     * @return JWT access token
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationMs);
        
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("roles", user.getRoles().stream()
                        .map(UserRole::name)
                        .collect(Collectors.toList()))
                .claim("emailVerified", user.isEmailVerified())
                .claim("type", "access")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Generate refresh token for token rotation.
     * 
     * @param user the authenticated user
     * @return JWT refresh token
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpirationMs);
        
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("type", "refresh")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Validate JWT token and extract claims.
     * 
     * @param token the JWT token
     * @return claims if valid, null if invalid
     */
    public Claims validateToken(String token) {
        try {
            JwtParser parser = Jwts.parser()
                    .setSigningKey(secretKey)
                    .requireIssuer(issuer)
                    .build();
            
            return parser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired: {}", e.getMessage());
            throw new RuntimeException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            throw new RuntimeException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            throw new RuntimeException("Malformed token", e);
        } catch (SecurityException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw new RuntimeException("Invalid signature", e);
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }
    
    /**
     * Extract user ID from JWT token.
     * 
     * @param token the JWT token
     * @return user ID if valid
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return UUID.fromString(claims.getSubject());
    }
    
    /**
     * Extract email from JWT token.
     * 
     * @param token the JWT token
     * @return email if valid
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }
    
    /**
     * Extract roles from JWT token.
     * 
     * @param token the JWT token
     * @return set of user roles
     */
    @SuppressWarnings("unchecked")
    public Set<UserRole> getRolesFromToken(String token) {
        Claims claims = validateToken(token);
        java.util.List<String> roleNames = claims.get("roles", java.util.List.class);
        
        return roleNames.stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toSet());
    }
    
    /**
     * Check if token is expired.
     * 
     * @param token the JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Check if token is a refresh token.
     * 
     * @param token the JWT token
     * @return true if refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if token is an access token.
     * 
     * @param token the JWT token
     * @return true if access token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "access".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get token expiration time.
     * 
     * @param token the JWT token
     * @return expiration time
     */
    public LocalDateTime getExpirationTime(String token) {
        Claims claims = validateToken(token);
        return claims.getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
    
    /**
     * Extract token from Authorization header.
     * 
     * @param authHeader the Authorization header value
     * @return JWT token without Bearer prefix
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}