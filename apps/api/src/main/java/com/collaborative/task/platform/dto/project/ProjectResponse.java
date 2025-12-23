package com.collaborative.task.platform.dto.project;

import com.collaborative.task.platform.entity.Project;
import com.collaborative.task.platform.entity.ProjectRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for project information.
 * Provides clean API response format for project data.
 */
public record ProjectResponse(
    UUID id,
    String name,
    String description,
    UUID ownerId,
    String ownerName,
    String ownerEmail,
    boolean isPrivate,
    boolean archived,
    int memberCount,
    ProjectRole userRole,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    /**
     * Create ProjectResponse from Project entity.
     */
    public static ProjectResponse from(Project project, ProjectRole userRole) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getOwner().getId(),
            project.getOwner().getFullName(),
            project.getOwner().getEmail(),
            project.isPrivate(),
            project.isArchived(),
            project.getMemberCount(),
            userRole,
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
    
    /**
     * Create ProjectResponse from Project entity without user role.
     */
    public static ProjectResponse from(Project project) {
        return from(project, null);
    }
}