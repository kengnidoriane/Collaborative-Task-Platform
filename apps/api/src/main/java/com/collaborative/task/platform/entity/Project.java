package com.collaborative.task.platform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Project entity representing collaborative projects in the system.
 * Implements clean domain modeling with comprehensive validation and team management.
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_owner", columnList = "owner_id"),
    @Index(name = "idx_projects_name", columnList = "name"),
    @Index(name = "idx_projects_created", columnList = "created_at")
})
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    @Column(nullable = false)
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull(message = "Project owner is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;
    
    @Column(name = "archived", nullable = false)
    private boolean archived = false;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ProjectMember> members = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Default constructor for JPA
    protected Project() {}
    
    // Constructor for project creation
    public Project(String name, String description, User owner, boolean isPrivate) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.isPrivate = isPrivate;
        this.archived = false;
        
        // Add owner as project member with OWNER role
        addMember(owner, ProjectRole.OWNER);
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    // Public setter for testing purposes
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public User getOwner() {
        return owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public boolean isPrivate() {
        return isPrivate;
    }
    
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
    
    public boolean isArchived() {
        return archived;
    }
    
    public void setArchived(boolean archived) {
        this.archived = archived;
    }
    
    public Set<ProjectMember> getMembers() {
        return members;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Business logic methods
    
    /**
     * Add a member to the project with specified role.
     */
    public void addMember(User user, ProjectRole role) {
        ProjectMember member = new ProjectMember(this, user, role);
        members.add(member);
    }
    
    /**
     * Remove a member from the project.
     */
    public void removeMember(User user) {
        members.removeIf(member -> member.getUser().equals(user));
    }
    
    /**
     * Check if user is a member of this project.
     */
    public boolean isMember(User user) {
        return members.stream()
                .anyMatch(member -> member.getUser().equals(user));
    }
    
    /**
     * Check if user is the owner of this project.
     */
    public boolean isOwner(User user) {
        return owner.equals(user);
    }
    
    /**
     * Check if user has admin privileges (owner or admin role).
     */
    public boolean hasAdminAccess(User user) {
        if (isOwner(user)) {
            return true;
        }
        return members.stream()
                .anyMatch(member -> member.getUser().equals(user) && 
                         member.getRole() == ProjectRole.ADMIN);
    }
    
    /**
     * Get member role for a specific user.
     */
    public ProjectRole getMemberRole(User user) {
        if (isOwner(user)) {
            return ProjectRole.OWNER;
        }
        return members.stream()
                .filter(member -> member.getUser().equals(user))
                .map(ProjectMember::getRole)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Update member role.
     */
    public void updateMemberRole(User user, ProjectRole newRole) {
        members.stream()
                .filter(member -> member.getUser().equals(user))
                .findFirst()
                .ifPresent(member -> member.setRole(newRole));
    }
    
    /**
     * Check if project is active (not archived).
     */
    public boolean isActive() {
        return !archived;
    }
    
    /**
     * Get total member count including owner.
     */
    public int getMemberCount() {
        return members.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project project)) return false;
        return id != null && id.equals(project.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", owner=" + (owner != null ? owner.getEmail() : "null") +
                ", isPrivate=" + isPrivate +
                ", archived=" + archived +
                ", memberCount=" + getMemberCount() +
                '}';
    }
}