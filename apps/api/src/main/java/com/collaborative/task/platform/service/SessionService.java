package com.collaborative.task.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for session management using Redis Streams.
 * Provides real-time session tracking and invalidation.
 */
@Service
public class SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Session TTL - 7 days for refresh tokens, 15 minutes for access tokens
    private static final Duration SESSION_TTL = Duration.ofDays(7);
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    
    public SessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Create a new session for a user.
     * 
     * @param userId the user ID
     * @param accessToken the access token
     * @param refreshToken the refresh token
     */
    public void createSession(UUID userId, String accessToken, String refreshToken) {
        String sessionKey = getSessionKey(userId);
        String accessTokenKey = getAccessTokenKey(userId);
        
        try {
            // Store session data
            SessionData sessionData = new SessionData(userId, accessToken, refreshToken, System.currentTimeMillis());
            redisTemplate.opsForValue().set(sessionKey, sessionData, SESSION_TTL);
            
            // Store access token separately for quick validation
            redisTemplate.opsForValue().set(accessTokenKey, accessToken, ACCESS_TOKEN_TTL);
            
            // Add to active sessions set
            redisTemplate.opsForSet().add("active_sessions", userId.toString());
            
            logger.debug("Session created for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Failed to create session for user: {}", userId, e);
            throw new RuntimeException("Session creation failed", e);
        }
    }
    
    /**
     * Update session with new tokens.
     * 
     * @param userId the user ID
     * @param newAccessToken the new access token
     * @param newRefreshToken the new refresh token
     */
    public void updateSession(UUID userId, String newAccessToken, String newRefreshToken) {
        String sessionKey = getSessionKey(userId);
        String accessTokenKey = getAccessTokenKey(userId);
        
        try {
            // Update session data
            SessionData sessionData = new SessionData(userId, newAccessToken, newRefreshToken, System.currentTimeMillis());
            redisTemplate.opsForValue().set(sessionKey, sessionData, SESSION_TTL);
            
            // Update access token
            redisTemplate.opsForValue().set(accessTokenKey, newAccessToken, ACCESS_TOKEN_TTL);
            
            logger.debug("Session updated for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Failed to update session for user: {}", userId, e);
            throw new RuntimeException("Session update failed", e);
        }
    }
    
    /**
     * Validate if session is active and tokens match.
     * 
     * @param userId the user ID
     * @param token the token to validate (access or refresh)
     * @return true if session is valid
     */
    public boolean isValidSession(UUID userId, String token) {
        String sessionKey = getSessionKey(userId);
        
        try {
            SessionData sessionData = (SessionData) redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionData == null) {
                return false;
            }
            
            // Check if token matches either access or refresh token
            return token.equals(sessionData.accessToken()) || token.equals(sessionData.refreshToken());
            
        } catch (Exception e) {
            logger.error("Failed to validate session for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Invalidate session for a user.
     * 
     * @param userId the user ID
     * @param token the token used for additional validation
     */
    public void invalidateSession(UUID userId, String token) {
        String sessionKey = getSessionKey(userId);
        String accessTokenKey = getAccessTokenKey(userId);
        
        try {
            // Verify token before invalidation
            if (isValidSession(userId, token)) {
                redisTemplate.delete(sessionKey);
                redisTemplate.delete(accessTokenKey);
                redisTemplate.opsForSet().remove("active_sessions", userId.toString());
                
                logger.debug("Session invalidated for user: {}", userId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to invalidate session for user: {}", userId, e);
        }
    }
    
    /**
     * Invalidate all sessions for a user.
     * Used when user changes password or account is compromised.
     * 
     * @param userId the user ID
     */
    public void invalidateAllSessions(UUID userId) {
        String sessionKey = getSessionKey(userId);
        String accessTokenKey = getAccessTokenKey(userId);
        
        try {
            redisTemplate.delete(sessionKey);
            redisTemplate.delete(accessTokenKey);
            redisTemplate.opsForSet().remove("active_sessions", userId.toString());
            
            logger.info("All sessions invalidated for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Failed to invalidate all sessions for user: {}", userId, e);
        }
    }
    
    /**
     * Get session data for a user.
     * 
     * @param userId the user ID
     * @return session data if exists
     */
    public SessionData getSessionData(UUID userId) {
        String sessionKey = getSessionKey(userId);
        
        try {
            return (SessionData) redisTemplate.opsForValue().get(sessionKey);
        } catch (Exception e) {
            logger.error("Failed to get session data for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Check if user has an active session.
     * 
     * @param userId the user ID
     * @return true if user has active session
     */
    public boolean hasActiveSession(UUID userId) {
        return redisTemplate.opsForSet().isMember("active_sessions", userId.toString());
    }
    
    /**
     * Get count of active sessions.
     * 
     * @return number of active sessions
     */
    public long getActiveSessionCount() {
        try {
            Long count = redisTemplate.opsForSet().size("active_sessions");
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Failed to get active session count", e);
            return 0;
        }
    }
    
    /**
     * Extend session TTL.
     * 
     * @param userId the user ID
     */
    public void extendSession(UUID userId) {
        String sessionKey = getSessionKey(userId);
        
        try {
            redisTemplate.expire(sessionKey, SESSION_TTL);
            logger.debug("Session extended for user: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to extend session for user: {}", userId, e);
        }
    }
    
    /**
     * Clean up expired sessions.
     * This method can be called periodically to clean up orphaned data.
     */
    public void cleanupExpiredSessions() {
        try {
            // Redis handles TTL automatically, but we can clean up the active_sessions set
            // This is a placeholder for any additional cleanup logic
            logger.debug("Session cleanup completed");
        } catch (Exception e) {
            logger.error("Failed to cleanup expired sessions", e);
        }
    }
    
    private String getSessionKey(UUID userId) {
        return "session:" + userId.toString();
    }
    
    private String getAccessTokenKey(UUID userId) {
        return "access_token:" + userId.toString();
    }
    
    /**
     * Session data record for Redis storage.
     */
    public record SessionData(
            UUID userId,
            String accessToken,
            String refreshToken,
            long createdAt
    ) {}
}