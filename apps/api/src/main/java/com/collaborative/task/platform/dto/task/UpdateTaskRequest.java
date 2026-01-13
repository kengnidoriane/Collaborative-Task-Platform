package com.collaborative.task.platform.dto.task;

import com.collaborative.task.platform.entity.TaskPriority;
import com.collaborative.task.platform.entity.TaskStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an existing task.
 * 
 * This DTO follows clean architecture principles with:
 * - Optional fields for partial updates
 * - Clear validation rules
 * - Immutable design using records
 */
public record UpdateTaskRequest(
    @Size(min = 1, max = 200, message = "Task title must be between 1 and 200 characters")
    String title,
    
    @Size(max = 2000, message = "Task description cannot exceed 2000 characters")
    String description,
    
    TaskStatus status,
    
    TaskPriority priority,
    
    UUID assigneeId,
    
    LocalDateTime dueDate,
    
    @Size(max = 10, message = "Maximum 10 tags allowed per task")
    List<String> tags,
    
    List<UUID> dependsOn,
    
    @Min(value = 0, message = "Estimated hours cannot be negative")
    @Max(value = 1000, message = "Estimated hours cannot exceed 1000")
    Integer estimatedHours,
    
    @Min(value = 0, message = "Actual hours cannot be negative")
    Integer actualHours
) {
    
    /**
     * Constructor with validation and normalization
     */
    public UpdateTaskRequest {
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
     * Creates an update request for status change only
     */
    public static UpdateTaskRequest statusOnly(TaskStatus status) {
        return new UpdateTaskRequest(
            null, null, status, null, null, null, null, null, null, null
        );
    }
    
    /**
     * Creates an update request for assignment change only
     */
    public static UpdateTaskRequest assignmentOnly(UUID assigneeId) {
        return new UpdateTaskRequest(
            null, null, null, null, assigneeId, null, null, null, null, null
        );
    }
    
    /**
     * Creates an update request for priority change only
     */
    public static UpdateTaskRequest priorityOnly(TaskPriority priority) {
        return new UpdateTaskRequest(
            null, null, null, priority, null, null, null, null, null, null
        );
    }
    
    /**
     * Creates an update request for basic info changes
     */
    public static UpdateTaskRequest basicInfo(String title, String description, LocalDateTime dueDate) {
        return new UpdateTaskRequest(
            title, description, null, null, null, dueDate, null, null, null, null
        );
    }
}