package com.collaborative.task.platform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * User entity representing authenticated users in the system.
 * Implements clean domain modeling with comprehensive validation.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_active", columnList = "id, lastLogin")
})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(unique = true, nullable = false)
    private String email;
    
    @Size(max = 255, message = "Password hash must not exceed 255 characters")
    @Column(name = "password_hash")
    private String passwordHash;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;
    
    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;
    
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<UserRole> roles = Set.of(UserRole.USER);
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Default constructor for JPA
    protected User() {}
    
    // Constructor for user registration
    public User(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
        this.emailVerified = false;
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public boolean isAccountLocked() {
        return accountLocked;
    }
    
    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }
    
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
    
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }
    
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.accountLocked = true;
        }
    }
    
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }
    
    public Set<UserRole> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<UserRole> roles) {
        this.roles = roles;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Business logic methods
    public boolean isActive() {
        return !accountLocked && emailVerified;
    }
    
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
    
    public void addRole(UserRole role) {
        this.roles.add(role);
    }
    
    public void removeRole(UserRole role) {
        this.roles.remove(role);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && id.equals(user.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", emailVerified=" + emailVerified +
                ", accountLocked=" + accountLocked +
                '}';
    }
}