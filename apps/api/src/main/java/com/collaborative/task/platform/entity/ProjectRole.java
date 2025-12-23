package com.collaborative.task.platform.entity;

/**
 * Enumeration of project roles defining access levels and permissions.
 * Implements hierarchical role system for project management.
 */
public enum ProjectRole {
    /**
     * Project owner - full administrative access, cannot be removed.
     */
    OWNER("Owner"),
    
    /**
     * Project administrator - can manage members and settings.
     */
    ADMIN("Administrator"),
    
    /**
     * Regular project member - can create and manage tasks.
     */
    MEMBER("Member"),
    
    /**
     * Read-only viewer - can view project but not modify.
     */
    VIEWER("Viewer");
    
    private final String displayName;
    
    ProjectRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this role has administrative privileges.
     */
    public boolean isAdmin() {
        return this == OWNER || this == ADMIN;
    }
    
    /**
     * Check if this role can modify project content.
     */
    public boolean canModify() {
        return this == OWNER || this == ADMIN || this == MEMBER;
    }
    
    /**
     * Check if this role can manage team members.
     */
    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }
    
    /**
     * Check if this role can modify project settings.
     */
    public boolean canModifySettings() {
        return this == OWNER || this == ADMIN;
    }
    
    /**
     * Check if this role can delete the project.
     */
    public boolean canDeleteProject() {
        return this == OWNER;
    }
}