package com.collaborative.task.platform.entity;

/**
 * User roles in the system.
 * Defines the different levels of access and permissions.
 */
public enum UserRole {
    USER("Standard user with basic permissions"),
    ADMIN("Administrator with full system access"),
    PROJECT_MANAGER("Project manager with enhanced project permissions");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}