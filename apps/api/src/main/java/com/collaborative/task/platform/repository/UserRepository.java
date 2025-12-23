package com.collaborative.task.platform.repository;

import com.collaborative.task.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 * Provides clean data access layer with optimized queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find user by email address.
     * Used for authentication and registration validation.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if email already exists in the system.
     * Used for registration validation.
     */
    boolean existsByEmail(String email);
    
    /**
     * Find active users (not locked and email verified).
     * Used for user management and analytics.
     */
    @Query("SELECT u FROM User u WHERE u.accountLocked = false AND u.emailVerified = true")
    java.util.List<User> findActiveUsers();
    
    /**
     * Find users who have logged in recently.
     * Used for analytics and user activity tracking.
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin > :since")
    java.util.List<User> findUsersLoggedInSince(@Param("since") LocalDateTime since);
    
    /**
     * Find users with failed login attempts above threshold.
     * Used for security monitoring and account protection.
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
    java.util.List<User> findUsersWithFailedAttempts(@Param("threshold") int threshold);
    
    /**
     * Find users whose passwords need to be changed.
     * Used for password policy enforcement.
     */
    @Query("SELECT u FROM User u WHERE u.passwordChangedAt < :before OR u.passwordChangedAt IS NULL")
    java.util.List<User> findUsersWithOldPasswords(@Param("before") LocalDateTime before);
}