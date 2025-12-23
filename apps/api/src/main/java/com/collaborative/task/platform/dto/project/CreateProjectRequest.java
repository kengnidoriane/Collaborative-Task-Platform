package com.collaborative.task.platform.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new project.
 * Implements validation for project creation requirements.
 */
public record CreateProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    String name,
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,
    
    boolean isPrivate
) {
    
    /**
     * Constructor with default values.
     */
    public CreateProjectRequest(String name, String description) {
        this(name, description, false);
    }
    
    /**
     * Constructor with name only.
     */
    public CreateProjectRequest(String name) {
        this(name, null, false);
    }
}