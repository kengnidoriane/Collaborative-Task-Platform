package com.collaborative.task.platform.dto.task;

import com.collaborative.task.platform.entity.TaskPriority;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new task.
 * 
 * This DTO follows clean architecture principles with:
 * - Clear validation rules
 * - Immutable design using records
 * - Comprehensive documentation
 */
public record CreateTaskRequest(
    @NotBlank(message = "Task title is required")
    @Size(min = 1, max = 200, message = "Task title must be between 1 and 200 characters")
    String title,
    
    @Size(max = 2000, message = "Task description cannot exceed 2000 characters")
    String description,
    
    @NotNull(message = "Project ID is required")
    UUID projectId,
    
    UUID assigneeId,
    
    @NotNull(message = "Task priority is required")
    TaskPriority priority,
    
    LocalDateTime dueDate,
    
    @Size(max = 10, message = "Maximum 10 tags allowed per task")
    List<String> tags,
    
    List<UUID> dependsOn,
    
    @Min(value = 0, message = "Estimated hours cannot be negative")
    @Max(value = 1000, message = "Estimated hours cannot exceed 1000")
    Integer estimatedHours
) {
    
    /**
     * Constructor with validation and normalization
     */
    public CreateTaskRequest {
        // Normalize tags by trimming whitespace and removing empty strings
        if (tags != null) {
            tags = tags.stream()
                      .filter(tag -> tag != null && !tag.trim().isEmpty())
                      .map(String::trim)
                      .distinct()
                      .toList();
        }
        
        // Ensure dependsOn list doesn't contain duplicates
        if (dependsOn != null) {
            dependsOn = dependsOn.stream()
                               .filter(id -> id != null)
                               .distinct()
                               .toList();
        }
    }
    
    /**
     * Creates a minimal task request with just title and project
     */
    public static CreateTaskRequest minimal(String title, UUID projectId) {
        return new CreateTaskRequest(
            title,
            null,
            projectId,
            null,
            TaskPriority.MEDIUM,
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Creates a task request with common fields
     */
    public static CreateTaskRequest withBasicInfo(String title, String description, 
                                                 UUID projectId, TaskPriority priority) {
        return new CreateTaskRequest(
            title,
            description,
            projectId,
            null,
            priority,
            null,
            null,
            null,
            null
        );
    }
}