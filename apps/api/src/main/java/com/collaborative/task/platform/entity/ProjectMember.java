package com.collaborative.task.platform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ProjectMember entity representing the relationship between users and projects.
 * Implements many-to-many relationship with role-based access control.
 */
@Entity
@Table(name = "project_members", indexes = {
    @Index(name = "idx_project_members_project", columnList = "project_id"),
    @Index(name = "idx_project_members_user", columnList = "user_id"),
    @Index(name = "idx_project_members_lookup", columnList = "project_id, user_id, role")
})
@IdClass(ProjectMemberId.class)
public class ProjectMember {
    
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull(message = "Project role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role = ProjectRole.MEMBER;
    
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "invited_by")
    private UUID invitedBy;
    
    @Column(name = "invitation_accepted_at")
    private LocalDateTime invitationAcceptedAt;
    
    // Default constructor for JPA
    protected ProjectMember() {}
    
    // Constructor for creating project membership
    public ProjectMember(Project project, User user, ProjectRole role) {
        this.project = project;
        this.user = user;
        this.role = role;
        this.invitationAcceptedAt = LocalDateTime.now();
    }
    
    // Constructor for creating project invitation
    public ProjectMember(Project project, User user, ProjectRole role, UUID invitedBy) {
        this.project = project;
        this.user = user;
        this.role = role;
        this.invitedBy = invitedBy;
        // invitationAcceptedAt will be null until invitation is accepted
    }
    
    // Getters and setters
    public Project getProject() {
        return project;
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public ProjectRole getRole() {
        return role;
    }
    
    public void setRole(ProjectRole role) {
        this.role = role;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public UUID getInvitedBy() {
        return invitedBy;
    }
    
    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }
    
    public LocalDateTime getInvitationAcceptedAt() {
        return invitationAcceptedAt;
    }
    
    public void setInvitationAcceptedAt(LocalDateTime invitationAcceptedAt) {
        this.invitationAcceptedAt = invitationAcceptedAt;
    }
    
    // Business logic methods
    
    /**
     * Check if invitation is pending (not yet accepted).
     */
    public boolean isPendingInvitation() {
        return invitationAcceptedAt == null;
    }
    
    /**
     * Accept the project invitation.
     */
    public void acceptInvitation() {
        this.invitationAcceptedAt = LocalDateTime.now();
    }
    
    /**
     * Check if member has administrative privileges.
     */
    public boolean hasAdminAccess() {
        return role.isAdmin();
    }
    
    /**
     * Check if member can modify project content.
     */
    public boolean canModify() {
        return role.canModify();
    }
    
    /**
     * Check if member can manage other members.
     */
    public boolean canManageMembers() {
        return role.canManageMembers();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMember that)) return false;
        return project != null && project.equals(that.project) &&
               user != null && user.equals(that.user);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "ProjectMember{" +
                "project=" + (project != null ? project.getName() : "null") +
                ", user=" + (user != null ? user.getEmail() : "null") +
                ", role=" + role +
                ", joinedAt=" + joinedAt +
                ", isPending=" + isPendingInvitation() +
                '}';
    }
}