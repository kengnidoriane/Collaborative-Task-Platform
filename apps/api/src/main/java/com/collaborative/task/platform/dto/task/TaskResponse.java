package com.collaborative.task.platform.dto.task;

import com.collaborative.task.platform.entity.Task;
import com.collaborative.task.platform.entity.TaskPriority;
import com.collaborative.task.platform.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for task data.
 * 
 * This DTO follows clean architecture principles with:
 * - Immutable design using records
 * - Clear data transformation from entity
 * - Comprehensive task information
 */
public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    UUID projectId,
    String projectName,
    AssigneeInfo assignee,
    CreatorInfo createdBy,
    LocalDateTime dueDate,
    List<String> tags,
    List<UUID> dependsOn,
    Integer estimatedHours,
    Integer actualHours,
    int completionPercentage,
    boolean isOverdue,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version
) {
    
    /**
     * Nested record for assignee information
     */
    public record AssigneeInfo(
        UUID id,
        String fullName,
        String email,
        String avatarUrl
    ) {
        public static AssigneeInfo from(com.collaborative.task.platform.entity.User user) {
            if (user == null) return null;
            return new AssigneeInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl()
            );
        }
    }
    
    /**
     * Nested record for creator information
     */
    public record CreatorInfo(
        UUID id,
        String fullName,
        String email
    ) {
        public static CreatorInfo from(com.collaborative.task.platform.entity.User user) {
            return new CreatorInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail()
            );
        }
    }
    
    /**
     * Creates a TaskResponse from a Task entity
     */
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getProject().getId(),
            task.getProject().getName(),
            AssigneeInfo.from(task.getAssignee()),
            CreatorInfo.from(task.getCreatedBy()),
            task.getDueDate(),
            task.getTags(),
            task.getDependsOn(),
            task.getEstimatedHours(),
            task.getActualHours(),
            task.getCompletionPercentage(),
            task.isOverdue(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            task.getVersion()
        );
    }
    
    /**
     * Creates a minimal TaskResponse with basic information
     */
    public static TaskResponse minimal(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            null, // description
            task.getStatus(),
            task.getPriority(),
            task.getProject().getId(),
            task.getProject().getName(),
            AssigneeInfo.from(task.getAssignee()),
            null, // createdBy
            task.getDueDate(),
            null, // tags
            null, // dependsOn
            null, // estimatedHours
            null, // actualHours
            task.getCompletionPercentage(),
            task.isOverdue(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            task.getVersion()
        );
    }
}