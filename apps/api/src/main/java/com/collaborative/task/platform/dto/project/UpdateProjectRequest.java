package com.collaborative.task.platform.dto.project;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating project information.
 * Implements validation for project update requirements.
 */
public record UpdateProjectRequest(
    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    String name,
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,
    
    Boolean isPrivate,
    
    Boolean archived
) {
    
    /**
     * Check if any field is provided for update.
     */
    public boolean hasUpdates() {
        return name != null || description != null || isPrivate != null || archived != null;
    }
}