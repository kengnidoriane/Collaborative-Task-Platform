package com.collaborative.task.platform.service;

import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for secure password hashing using Argon2id.
 * Provides stronger security than bcrypt with configurable parameters.
 */
@Service
public class PasswordService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);
    
    private final de.mkammerer.argon2.Argon2 argon2;
    
    // Argon2id parameters - balanced for security and performance
    private static final int ITERATIONS = 3;
    private static final int MEMORY_KB = 65536; // 64 MB
    private static final int PARALLELISM = 1;
    
    public PasswordService() {
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        
        // Find optimal iteration count for current hardware (target: ~500ms)
        int optimalIterations = Argon2Helper.findIterations(argon2, 500, MEMORY_KB, PARALLELISM);
        logger.info("Optimal Argon2id iterations for this hardware: {}", optimalIterations);
    }
    
    /**
     * Hash a password using Argon2id.
     * 
     * @param password the plain text password
     * @return the hashed password
     */
    public String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        
        try {
            String hash = argon2.hash(ITERATIONS, MEMORY_KB, PARALLELISM, password.toCharArray());
            logger.debug("Password hashed successfully");
            return hash;
        } catch (Exception e) {
            logger.error("Failed to hash password", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    
    /**
     * Verify a password against its hash.
     * 
     * @param password the plain text password
     * @param hash the stored hash
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        
        try {
            boolean matches = argon2.verify(hash, password.toCharArray());
            logger.debug("Password verification: {}", matches ? "success" : "failed");
            return matches;
        } catch (Exception e) {
            logger.error("Password verification failed", e);
            return false;
        }
    }
    
    /**
     * Check if a password hash needs to be updated.
     * This can happen when security parameters change.
     * 
     * @param hash the stored hash
     * @return true if hash should be updated
     */
    public boolean needsRehash(String hash) {
        if (hash == null || hash.isBlank()) {
            return true;
        }
        
        try {
            // Check if hash uses current parameters
            return !hash.startsWith("$argon2id$v=19$m=" + MEMORY_KB + ",t=" + ITERATIONS + ",p=" + PARALLELISM);
        } catch (Exception e) {
            logger.warn("Could not parse hash format, recommending rehash", e);
            return true;
        }
    }
    
    /**
     * Validate password strength.
     * 
     * @param password the password to validate
     * @return true if password meets strength requirements
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * Generate password strength score (0-100).
     * 
     * @param password the password to score
     * @return strength score from 0 (weak) to 100 (strong)
     */
    public int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // Length scoring
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 15;
        if (password.length() >= 16) score += 10;
        
        // Character variety scoring
        if (password.chars().anyMatch(Character::isLowerCase)) score += 10;
        if (password.chars().anyMatch(Character::isUpperCase)) score += 10;
        if (password.chars().anyMatch(Character::isDigit)) score += 10;
        if (password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0)) score += 15;
        
        // Uniqueness scoring (no repeated patterns)
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars >= password.length() * 0.7) score += 5;
        
        return Math.min(100, score);
    }
}