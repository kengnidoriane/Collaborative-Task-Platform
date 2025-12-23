package com.collaborative.task.platform.repository;

import com.collaborative.task.platform.entity.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WebAuthn credential operations.
 * Provides data access for passkey authentication.
 */
@Repository
public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, UUID> {
    
    /**
     * Find credential by credential ID.
     * Used during WebAuthn authentication.
     */
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
    
    /**
     * Find all active credentials for a user.
     * Used for credential management and authentication options.
     */
    @Query("SELECT c FROM WebAuthnCredential c WHERE c.userId = :userId AND c.active = true")
    List<WebAuthnCredential> findActiveCredentialsByUserId(@Param("userId") UUID userId);
    
    /**
     * Find all credentials for a user (including inactive).
     * Used for credential management interface.
     */
    List<WebAuthnCredential> findByUserId(UUID userId);
    
    /**
     * Check if user has any active credentials.
     * Used to determine if passwordless login is available.
     */
    @Query("SELECT COUNT(c) > 0 FROM WebAuthnCredential c WHERE c.userId = :userId AND c.active = true")
    boolean hasActiveCredentials(@Param("userId") UUID userId);
    
    /**
     * Count active credentials for a user.
     * Used for credential limits and management.
     */
    @Query("SELECT COUNT(c) FROM WebAuthnCredential c WHERE c.userId = :userId AND c.active = true")
    long countActiveCredentialsByUserId(@Param("userId") UUID userId);
}