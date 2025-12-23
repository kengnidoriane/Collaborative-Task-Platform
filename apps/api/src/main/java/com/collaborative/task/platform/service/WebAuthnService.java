package com.collaborative.task.platform.service;

import com.collaborative.task.platform.entity.User;
import com.collaborative.task.platform.entity.WebAuthnCredential;
import com.collaborative.task.platform.repository.WebAuthnCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for WebAuthn (passkey) authentication.
 * Simplified implementation for MVP - full WebAuthn integration to be completed later.
 */
@Service
public class WebAuthnService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebAuthnService.class);
    
    private final WebAuthnCredentialRepository credentialRepository;
    private final String rpId;
    private final String rpName;
    private final String origin;
    
    public WebAuthnService(
            WebAuthnCredentialRepository credentialRepository,
            @Value("${app.webauthn.rp-id:localhost}") String rpId,
            @Value("${app.webauthn.rp-name:Collaborative Task Platform}") String rpName,
            @Value("${app.webauthn.origin:http://localhost:3000}") String origin) {
        
        this.credentialRepository = credentialRepository;
        this.rpId = rpId;
        this.rpName = rpName;
        this.origin = origin;
        
        logger.info("WebAuthn service initialized for RP: {} ({})", rpName, rpId);
    }
    
    /**
     * Check if user has WebAuthn credentials.
     * 
     * @param userId the user ID
     * @return true if user has active credentials
     */
    public boolean hasCredentials(UUID userId) {
        return credentialRepository.hasActiveCredentials(userId);
    }
    
    /**
     * Delete a credential.
     * 
     * @param credentialId the credential ID to delete
     * @param userId the user ID (for authorization)
     */
    public void deleteCredential(String credentialId, UUID userId) {
        Optional<WebAuthnCredential> credential = credentialRepository.findByCredentialId(credentialId);
        if (credential.isPresent() && credential.get().getUserId().equals(userId)) {
            credential.get().deactivate();
            credentialRepository.save(credential.get());
            logger.info("WebAuthn credential deactivated: {}", credentialId);
        } else {
            throw new RuntimeException("Credential not found or unauthorized");
        }
    }
    
    /**
     * Placeholder for WebAuthn registration start.
     * To be implemented with full WebAuthn library integration.
     */
    public String startRegistration(User user, String displayName) {
        logger.info("WebAuthn registration start requested for user: {}", user.getEmail());
        // Return placeholder response
        return "{}";
    }
    
    /**
     * Placeholder for WebAuthn registration finish.
     * To be implemented with full WebAuthn library integration.
     */
    public WebAuthnCredential finishRegistration(User user, String credentialData, String displayName) {
        logger.info("WebAuthn registration finish requested for user: {}", user.getEmail());
        
        // Create a placeholder credential for testing
        WebAuthnCredential credential = new WebAuthnCredential(
                user.getId(),
                "placeholder-credential-id",
                "placeholder-public-key",
                displayName
        );
        
        return credentialRepository.save(credential);
    }
    
    /**
     * Placeholder for WebAuthn authentication start.
     * To be implemented with full WebAuthn library integration.
     */
    public String startAuthentication(String email) {
        logger.info("WebAuthn authentication start requested for email: {}", email);
        // Return placeholder response
        return "{}";
    }
    
    /**
     * Placeholder for WebAuthn authentication finish.
     * To be implemented with full WebAuthn library integration.
     */
    public WebAuthnCredential finishAuthentication(String credentialData, String email) {
        logger.info("WebAuthn authentication finish requested for email: {}", email);
        
        // For testing, return the first active credential for the user
        // In real implementation, this would validate the WebAuthn assertion
        return credentialRepository.findActiveCredentialsByUserId(UUID.randomUUID())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No credentials found"));
    }
}