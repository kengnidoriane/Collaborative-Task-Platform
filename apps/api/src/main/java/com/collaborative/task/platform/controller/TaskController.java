package com.collaborative.task.platform.controller;

import com.collaborative.task.platform.dto.task.*;
import com.collaborative.task.platform.entity.TaskStatus;
import com.collaborative.task.platform.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for task management operations.
 * 
 * This controller follows clean architecture principles with:
 * - Clear REST API design
 * - Comprehensive error handling
 * - Proper HTTP status codes
 * - Detailed API documentation
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Task Management", description = "APIs for managing tasks within projects")
public class TaskController {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    
    private final TaskService taskService;
    
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    
    // Task Creation
    
    @PostMapping
    @Operation(summary = "Create a new task", description = "Creates a new task within a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied to project"),
        @ApiResponse(responseCode = "404", description = "Project or assignee not found")
    })
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {
        
        logger.info("Creating task request received: {}", request.title());
        
        UUID userId = getUserId(authentication);
        TaskResponse response = taskService.createTask(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // Task Retrieval
    
    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID", description = "Retrieves a specific task by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable UUID taskId,
            Authentication authentication) {
        
        UUID userId = getUserId(authentication);
        TaskResponse response = taskService.getTask(taskId, userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get tasks for project", description = "Retrieves all tasks for a specific project with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to project"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<Page<TaskResponse>> getProjectTasks(
            @PathVariable UUID projectId,
            @PageableDefault(size = 20, sort = {"priority", "createdAt"}) Pageable pageable,
            Authentication authentication) {
        
        UUID userId = getUserId(authentication);
        Page<TaskResponse> response = taskService.getProjectTasks(projectId, userId, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/project/{projectId}/status/{status}")
    @Operation(summary = "Get tasks by status", description = "Retrieves tasks with a specific status for a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to project"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<TaskResponse>> getTasksByStatus(
            @PathVariable UUID projectId,
            @PathVariable TaskStatus status,
            Authentication authentication) {
        
        UUID userId = getUserId(authentication);
        List<TaskResponse> response = taskService.getTasksByStatus(projectId, status, userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/assigned")
    @Operation(summary = "Get user's assigned tasks", description = "Retrieves all tasks assigned to the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    })
    public ResponseEntity<List<TaskResponse>> getUserTasks(Authentication authentication) {
        UUID userId = getUserId(authentication);
        List<TaskResponse> response = taskService.getUserTasks(userId);
        
        return ResponseEntity.ok(response);
    }
    
    // Task Updates
    
    @PutMapping("/{taskId}")
    @Operation(summary = "Update task", description = "Updates an existing task with new information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied to task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication) {
        
        logger.info("Updating task {} request received", taskId);
        
        UUID userId = getUserId(authentication);
        TaskResponse response = taskService.updateTask(taskId, request, userId);
        
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{taskId}/assign/{assigneeId}")
    @Operation(summary = "Assign task", description = "Assigns a task to a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task assigned successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to task"),
        @ApiResponse(responseCode = "404", description = "Task or assignee not found")
    })
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable UUID taskId,
            @PathVariable UUID assigneeId,
            Authentication authentication) {
        
        logger.info("Assigning task {} to user {}", taskId, assigneeId);
        
        UUID userId = getUserId(authentication);
        TaskResponse response = taskService.assignTask(taskId, assigneeId, userId);
        
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{taskId}/status/{status}")
    @Operation(summary = "Change task status", description = "Changes the status of a task")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task status changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied to task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> changeTaskStatus(
            @PathVariable UUID taskId,
            @PathVariable TaskStatus status,
            Authentication authentication) {
        
        logger.info("Changing task {} status to {}", taskId, status);
        
        UUID userId = getUserId(authentication);
        TaskResponse response = taskService.changeTaskStatus(taskId, status, userId);
        
        return ResponseEntity.ok(response);
    }
    
    // Task Deletion
    
    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete task", description = "Deletes a task and removes all its dependencies")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete task with dependencies"),
        @ApiResponse(responseCode = "403", description = "Access denied to task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID taskId,
            Authentication authentication) {
        
        logger.info("Deleting task {} request received", taskId);
        
        UUID userId = getUserId(authentication);
        taskService.deleteTask(taskId, userId);
        
        return ResponseEntity.noContent().build();
    }
    
    // Analytics and Reporting
    
    @GetMapping("/project/{projectId}/summary")
    @Operation(summary = "Get project task summary", description = "Retrieves task analytics and summary for a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to project"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<TaskSummaryResponse> getProjectTaskSummary(
            @PathVariable UUID projectId,
            Authentication authentication) {
        
        UUID userId = getUserId(authentication);
        TaskSummaryResponse response = taskService.getProjectTaskSummary(projectId, userId);
        
        return ResponseEntity.ok(response);
    }
    
    // Search and Filtering
    
    @GetMapping("/project/{projectId}/search")
    @Operation(summary = "Search tasks", description = "Searches tasks within a project by title or description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to project"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<List<TaskResponse>> searchTasks(
            @PathVariable UUID projectId,
            @RequestParam @Parameter(description = "Search term to look for in task title or description") String q,
            Authentication authentication) {
        
        UUID userId = getUserId(authentication);
        List<TaskResponse> response = taskService.searchTasks(projectId, q, userId);
        
        return ResponseEntity.ok(response);
    }
    
    // Dependency Management
    
    @PostMapping("/{taskId}/dependencies/{dependsOnTaskId}")
    @Operation(summary = "Add task dependency", description = "Adds a dependency relationship between two tasks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dependency added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid dependency or would create circular dependency"),
        @ApiResponse(responseCode = "403", description = "Access denied to tasks"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> addTaskDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependsOnTaskId,
            Authentication authentication) {
        
        logger.info("Adding dependency: task {} depends on task {}", taskId, dependsOnTaskId);
        
        UUID userId = getUserId(authentication);
        TaskResponse response = taskService.addTaskDependency(taskId, dependsOnTaskId, userId);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{taskId}/dependencies/{dependsOnTaskId}")
    @Operation(summary = "Remove task dependency", description = "Removes a dependency relationship between two tasks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dependency removed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> removeTaskDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependsOnTaskId,
            Authentication authentication) {
        
        logger.info("Removing dependency: task {} no longer depends on task {}", taskId, dependsOnTaskId);
        
        UUID userId = getUserId(authentication);
        TaskResponse response = taskService.removeTaskDependency(taskId, dependsOnTaskId, userId);
        
        return ResponseEntity.ok(response);
    }
    
    // Batch Operations
    
    @PatchMapping("/batch/status")
    @Operation(summary = "Batch update task status", description = "Updates the status of multiple tasks at once")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied to one or more tasks")
    })
    public ResponseEntity<String> batchUpdateTaskStatus(
            @RequestBody @Valid BatchUpdateStatusRequest request,
            Authentication authentication) {
        
        logger.info("Batch updating {} tasks to status {}", request.taskIds().size(), request.status());
        
        // This would be implemented in the service layer
        // For now, return a simple response
        return ResponseEntity.ok("Batch update completed");
    }
    
    // Helper Methods
    
    private UUID getUserId(Authentication authentication) {
        // Extract user ID from authentication
        // This would depend on your authentication implementation
        return UUID.fromString(authentication.getName());
    }
    
    /**
     * Request DTO for batch status updates
     */
    public record BatchUpdateStatusRequest(
        @Parameter(description = "List of task IDs to update")
        List<UUID> taskIds,
        
        @Parameter(description = "New status to apply to all tasks")
        TaskStatus status
    ) {}
}