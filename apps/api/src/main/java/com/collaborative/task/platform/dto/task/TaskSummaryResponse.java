package com.collaborative.task.platform.dto.task;

import com.collaborative.task.platform.entity.TaskPriority;
import com.collaborative.task.platform.entity.TaskStatus;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for task summary and analytics.
 * 
 * This DTO provides aggregated task information for dashboards and reports.
 */
public record TaskSummaryResponse(
    UUID projectId,
    String projectName,
    long totalTasks,
    long completedTasks,
    long overdueTasks,
    double completionRate,
    double averageHoursPerTask,
    Map<TaskStatus, Long> tasksByStatus,
    Map<TaskPriority, Long> tasksByPriority,
    Map<String, Long> tasksByAssignee
) {
    
    /**
     * Calculates completion rate as a percentage
     */
    public double getCompletionPercentage() {
        return totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;
    }
    
    /**
     * Checks if the project has overdue tasks
     */
    public boolean hasOverdueTasks() {
        return overdueTasks > 0;
    }
    
    /**
     * Gets the most common task status
     */
    public TaskStatus getMostCommonStatus() {
        return tasksByStatus.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(TaskStatus.TODO);
    }
    
    /**
     * Gets the most common task priority
     */
    public TaskPriority getMostCommonPriority() {
        return tasksByPriority.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(TaskPriority.MEDIUM);
    }
}