package com.collaborative.task.platform.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for ProjectMember entity.
 * Implements Serializable for JPA composite key requirements.
 */
public class ProjectMemberId implements Serializable {
    
    private UUID project;
    private UUID user;
    
    // Default constructor
    public ProjectMemberId() {}
    
    // Constructor with parameters
    public ProjectMemberId(UUID project, UUID user) {
        this.project = project;
        this.user = user;
    }
    
    // Getters and setters
    public UUID getProject() {
        return project;
    }
    
    public void setProject(UUID project) {
        this.project = project;
    }
    
    public UUID getUser() {
        return user;
    }
    
    public void setUser(UUID user) {
        this.user = user;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMemberId that)) return false;
        return Objects.equals(project, that.project) && 
               Objects.equals(user, that.user);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(project, user);
    }
    
    @Override
    public String toString() {
        return "ProjectMemberId{" +
                "project=" + project +
                ", user=" + user +
                '}';
    }
}