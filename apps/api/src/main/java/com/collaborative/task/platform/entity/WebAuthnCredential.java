package com.collaborative.task.platform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebAuthn credential entity for passkey authentication.
 * Stores public key credentials for passwordless authentication.
 */
@Entity
@Table(name = "webauthn_credentials", indexes = {
    @Index(name = "idx_webauthn_credential_id", columnList = "credentialId", unique = true),
    @Index(name = "idx_webauthn_user_id", columnList = "userId")
})
public class WebAuthnCredential {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @NotBlank(message = "Credential ID is required")
    @Column(name = "credential_id", nullable = false, unique = true, length = 1024)
    private String credentialId;
    
    @NotBlank(message = "Public key is required")
    @Column(name = "public_key", nullable = false, length = 2048)
    private String publicKey;
    
    @NotNull(message = "Signature count is required")
    @Column(name = "signature_count", nullable = false)
    private Long signatureCount = 0L;
    
    @Column(name = "aaguid", length = 36)
    private String aaguid;
    
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    @Column(name = "last_used")
    private LocalDateTime lastUsed;
    
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Default constructor for JPA
    protected WebAuthnCredential() {}
    
    // Constructor for credential registration
    public WebAuthnCredential(UUID userId, String credentialId, String publicKey, String displayName) {
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.displayName = displayName;
        this.signatureCount = 0L;
        this.active = true;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getCredentialId() {
        return credentialId;
    }
    
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public Long getSignatureCount() {
        return signatureCount;
    }
    
    public void setSignatureCount(Long signatureCount) {
        this.signatureCount = signatureCount;
    }
    
    public String getAaguid() {
        return aaguid;
    }
    
    public void setAaguid(String aaguid) {
        this.aaguid = aaguid;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Business logic methods
    public void updateSignatureCount(Long newCount) {
        this.signatureCount = newCount;
        this.lastUsed = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebAuthnCredential that)) return false;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "WebAuthnCredential{" +
                "id=" + id +
                ", userId=" + userId +
                ", credentialId='" + credentialId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", active=" + active +
                '}';
    }
}