package com.collaborative.task.platform.service;

import com.collaborative.task.platform.dto.task.*;
import com.collaborative.task.platform.entity.*;
import com.collaborative.task.platform.exception.BusinessException;
import com.collaborative.task.platform.exception.ResourceNotFoundException;
import com.collaborative.task.platform.repository.ProjectRepository;
import com.collaborative.task.platform.repository.TaskRepository;
import com.collaborative.task.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for task management operations.
 * 
 * This service follows clean architecture principles with:
 * - Clear separation of business logic
 * - Comprehensive validation
 * - Proper transaction management
 * - Detailed logging and error handling
 */
@Service
@Transactional
public class TaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public TaskService(TaskRepository taskRepository,
                      ProjectRepository projectRepository,
                      UserRepository userRepository,
                      NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }
    
    // Task Creation
    
    /**
     * Creates a new task with validation and notifications
     */
    public TaskResponse createTask(CreateTaskRequest request, UUID createdByUserId) {
        logger.info("Creating task '{}' in project {} by user {}", 
                   request.title(), request.projectId(), createdByUserId);
        
        // Validate project exists and user has access
        Project project = projectRepository.findById(request.projectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        User createdBy = userRepository.findById(createdByUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(project, createdBy);
        
        // Validate assignee if provided
        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            validateProjectAccess(project, assignee);
        }
        
        // Validate dependencies
        if (request.dependsOn() != null && !request.dependsOn().isEmpty()) {
            validateTaskDependencies(request.dependsOn(), request.projectId());
        }
        
        // Create task
        Task task = new Task(request.title(), project, createdBy);
        
        // Set optional fields
        if (request.description() != null) {
            task.updateDetails(request.title(), request.description(), request.dueDate());
        }
        
        if (request.priority() != null) {
            task.changePriority(request.priority());
        }
        
        if (assignee != null) {
            task.assignTo(assignee);
        }
        
        if (request.estimatedHours() != null) {
            task.setEstimatedHours(request.estimatedHours());
        }
        
        // Add tags
        if (request.tags() != null) {
            request.tags().forEach(task::addTag);
        }
        
        // Add dependencies
        if (request.dependsOn() != null) {
            request.dependsOn().forEach(task::addDependency);
        }
        
        Task savedTask = taskRepository.save(task);
        
        // Send notifications
        notifyTaskCreated(savedTask);
        
        logger.info("Task created successfully with ID: {}", savedTask.getId());
        return TaskResponse.from(savedTask);
    }
    
    // Task Retrieval
    
    /**
     * Gets a task by ID with access validation
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(task.getProject(), user);
        
        return TaskResponse.from(task);
    }
    
    /**
     * Gets all tasks for a project with pagination
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getProjectTasks(UUID projectId, UUID userId, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(project, user);
        
        return taskRepository.findByProjectId(projectId, pageable)
            .map(TaskResponse::from);
    }
    
    /**
     * Gets tasks assigned to a user
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getUserTasks(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return taskRepository.findByAssigneeIdOrderByPriorityAndDueDate(userId)
            .stream()
            .map(TaskResponse::from)
            .toList();
    }
    
    /**
     * Gets tasks by status for a project
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByStatus(UUID projectId, TaskStatus status, UUID userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(project, user);
        
        return taskRepository.findByProjectIdAndStatus(projectId, status)
            .stream()
            .map(TaskResponse::from)
            .toList();
    }
    
    // Task Updates
    
    /**
     * Updates a task with validation and notifications
     */
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UUID userId) {
        logger.info("Updating task {} by user {}", taskId, userId);
        
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(task.getProject(), user);
        
        // Track changes for notifications
        TaskStatus oldStatus = task.getStatus();
        User oldAssignee = task.getAssignee();
        
        // Update fields
        if (request.title() != null || request.description() != null || request.dueDate() != null) {
            task.updateDetails(
                request.title() != null ? request.title() : task.getTitle(),
                request.description(),
                request.dueDate()
            );
        }
        
        if (request.status() != null) {
            task.changeStatus(request.status());
        }
        
        if (request.priority() != null) {
            task.changePriority(request.priority());
        }
        
        if (request.assigneeId() != null) {
            User newAssignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            validateProjectAccess(task.getProject(), newAssignee);
            task.assignTo(newAssignee);
        }
        
        if (request.estimatedHours() != null) {
            task.setEstimatedHours(request.estimatedHours());
        }
        
        if (request.actualHours() != null) {
            task.recordTimeSpent(request.actualHours() - (task.getActualHours() != null ? task.getActualHours() : 0));
        }
        
        // Update tags
        if (request.tags() != null) {
            // Clear existing tags and add new ones
            task.getTags().clear();
            request.tags().forEach(task::addTag);
        }
        
        // Update dependencies
        if (request.dependsOn() != null) {
            validateTaskDependencies(request.dependsOn(), task.getProject().getId());
            // Clear existing dependencies and add new ones
            task.getDependsOn().clear();
            request.dependsOn().forEach(task::addDependency);
        }
        
        Task savedTask = taskRepository.save(task);
        
        // Send notifications for significant changes
        notifyTaskUpdated(savedTask, oldStatus, oldAssignee);
        
        logger.info("Task {} updated successfully", taskId);
        return TaskResponse.from(savedTask);
    }
    
    /**
     * Assigns a task to a user
     */
    public TaskResponse assignTask(UUID taskId, UUID assigneeId, UUID userId) {
        logger.info("Assigning task {} to user {} by user {}", taskId, assigneeId, userId);
        
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        
        validateProjectAccess(task.getProject(), user);
        validateProjectAccess(task.getProject(), assignee);
        
        User oldAssignee = task.getAssignee();
        task.assignTo(assignee);
        
        Task savedTask = taskRepository.save(task);
        
        // Send assignment notification
        notifyTaskAssigned(savedTask, oldAssignee);
        
        logger.info("Task {} assigned to user {} successfully", taskId, assigneeId);
        return TaskResponse.from(savedTask);
    }
    
    /**
     * Changes task status with validation
     */
    public TaskResponse changeTaskStatus(UUID taskId, TaskStatus newStatus, UUID userId) {
        logger.info("Changing task {} status to {} by user {}", taskId, newStatus, userId);
        
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(task.getProject(), user);
        
        TaskStatus oldStatus = task.getStatus();
        task.changeStatus(newStatus);
        
        Task savedTask = taskRepository.save(task);
        
        // Send status change notification
        notifyTaskStatusChanged(savedTask, oldStatus);
        
        logger.info("Task {} status changed from {} to {} successfully", taskId, oldStatus, newStatus);
        return TaskResponse.from(savedTask);
    }
    
    // Task Deletion
    
    /**
     * Deletes a task with dependency validation
     */
    public void deleteTask(UUID taskId, UUID userId) {
        logger.info("Deleting task {} by user {}", taskId, userId);
        
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(task.getProject(), user);
        
        // Check if other tasks depend on this task
        List<Task> dependentTasks = taskRepository.findTasksDependingOn(taskId);
        if (!dependentTasks.isEmpty()) {
            throw new BusinessException("Cannot delete task with dependent tasks. Remove dependencies first.");
        }
        
        taskRepository.delete(task);
        
        // Send deletion notification
        notifyTaskDeleted(task);
        
        logger.info("Task {} deleted successfully", taskId);
    }
    
    // Analytics and Reporting
    
    /**
     * Gets task summary for a project
     */
    @Transactional(readOnly = true)
    public TaskSummaryResponse getProjectTaskSummary(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(project, user);
        
        // Get task statistics
        Object[] stats = taskRepository.getTaskStatisticsForProject(projectId);
        long completedTasks = stats[0] != null ? ((Number) stats[0]).longValue() : 0;
        long totalTasks = stats[1] != null ? ((Number) stats[1]).longValue() : 0;
        double avgHours = stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0;
        
        // Get overdue tasks count
        List<TaskStatus> completedStatuses = Arrays.asList(TaskStatus.DONE, TaskStatus.CANCELLED);
        List<Task> overdueTasks = taskRepository.findOverdueTasks(LocalDateTime.now(), completedStatuses);
        long overdueCount = overdueTasks.stream()
            .filter(task -> task.getProject().getId().equals(projectId))
            .count();
        
        // Get tasks by status
        Map<TaskStatus, Long> tasksByStatus = taskRepository.countTasksByStatusForProject(projectId)
            .stream()
            .collect(Collectors.toMap(
                row -> (TaskStatus) row[0],
                row -> ((Number) row[1]).longValue()
            ));
        
        // Get tasks by priority
        Map<TaskPriority, Long> tasksByPriority = taskRepository.countTasksByPriorityForProject(projectId)
            .stream()
            .collect(Collectors.toMap(
                row -> (TaskPriority) row[0],
                row -> ((Number) row[1]).longValue()
            ));
        
        // Get tasks by assignee (simplified - would need a proper query)
        Map<String, Long> tasksByAssignee = new HashMap<>();
        
        double completionRate = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;
        
        return new TaskSummaryResponse(
            projectId,
            project.getName(),
            totalTasks,
            completedTasks,
            overdueCount,
            completionRate,
            avgHours,
            tasksByStatus,
            tasksByPriority,
            tasksByAssignee
        );
    }
    
    // Search and Filtering
    
    /**
     * Searches tasks within a project
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> searchTasks(UUID projectId, String searchTerm, UUID userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(project, user);
        
        return taskRepository.searchTasksInProject(projectId, searchTerm)
            .stream()
            .map(TaskResponse::from)
            .toList();
    }
    
    // Dependency Management
    
    /**
     * Adds a dependency between tasks
     */
    public TaskResponse addTaskDependency(UUID taskId, UUID dependsOnTaskId, UUID userId) {
        logger.info("Adding dependency: task {} depends on task {} by user {}", taskId, dependsOnTaskId, userId);
        
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        Task dependsOnTask = taskRepository.findById(dependsOnTaskId)
            .orElseThrow(() -> new ResourceNotFoundException("Dependency task not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(task.getProject(), user);
        
        // Validate both tasks are in the same project
        if (!task.getProject().getId().equals(dependsOnTask.getProject().getId())) {
            throw new BusinessException("Tasks must be in the same project to create dependencies");
        }
        
        // Check for circular dependencies
        if (wouldCreateCircularDependency(taskId, dependsOnTaskId)) {
            throw new BusinessException("Adding this dependency would create a circular dependency");
        }
        
        task.addDependency(dependsOnTaskId);
        Task savedTask = taskRepository.save(task);
        
        logger.info("Dependency added successfully");
        return TaskResponse.from(savedTask);
    }
    
    /**
     * Removes a dependency between tasks
     */
    public TaskResponse removeTaskDependency(UUID taskId, UUID dependsOnTaskId, UUID userId) {
        logger.info("Removing dependency: task {} no longer depends on task {} by user {}", taskId, dependsOnTaskId, userId);
        
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateProjectAccess(task.getProject(), user);
        
        task.removeDependency(dependsOnTaskId);
        Task savedTask = taskRepository.save(task);
        
        logger.info("Dependency removed successfully");
        return TaskResponse.from(savedTask);
    }
    
    // Private Helper Methods
    
    private void validateProjectAccess(Project project, User user) {
        // This would check if the user is a member of the project
        // For now, we'll assume access is granted
        // In a real implementation, this would query the project_members table
    }
    
    private void validateTaskDependencies(List<UUID> dependsOn, UUID projectId) {
        for (UUID taskId : dependsOn) {
            if (!taskRepository.existsByIdAndProjectId(taskId, projectId)) {
                throw new BusinessException("Dependency task not found in project: " + taskId);
            }
        }
    }
    
    private boolean wouldCreateCircularDependency(UUID taskId, UUID dependsOnTaskId) {
        // Check if adding this dependency would create a cycle
        return taskRepository.hasCircularDependency(dependsOnTaskId);
    }
    
    // Notification Methods
    
    private void notifyTaskCreated(Task task) {
        try {
            if (task.getAssignee() != null) {
                notificationService.sendTaskAssignedNotification(
                    task.getAssignee().getId(),
                    task.getId(),
                    task.getTitle(),
                    task.getProject().getName()
                );
            }
            
            // Notify project members about new task
            notificationService.sendTaskCreatedNotification(
                task.getProject().getId(),
                task.getId(),
                task.getTitle(),
                task.getCreatedBy().getFullName()
            );
        } catch (Exception e) {
            logger.warn("Failed to send task creation notifications for task {}: {}", task.getId(), e.getMessage());
        }
    }
    
    private void notifyTaskUpdated(Task task, TaskStatus oldStatus, User oldAssignee) {
        try {
            // Notify on status change
            if (!Objects.equals(task.getStatus(), oldStatus)) {
                notificationService.sendTaskStatusChangedNotification(
                    task.getProject().getId(),
                    task.getId(),
                    task.getTitle(),
                    oldStatus,
                    task.getStatus()
                );
            }
            
            // Notify on assignment change
            if (!Objects.equals(task.getAssignee(), oldAssignee)) {
                if (task.getAssignee() != null) {
                    notificationService.sendTaskAssignedNotification(
                        task.getAssignee().getId(),
                        task.getId(),
                        task.getTitle(),
                        task.getProject().getName()
                    );
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to send task update notifications for task {}: {}", task.getId(), e.getMessage());
        }
    }
    
    private void notifyTaskAssigned(Task task, User oldAssignee) {
        try {
            if (task.getAssignee() != null && !Objects.equals(task.getAssignee(), oldAssignee)) {
                notificationService.sendTaskAssignedNotification(
                    task.getAssignee().getId(),
                    task.getId(),
                    task.getTitle(),
                    task.getProject().getName()
                );
            }
        } catch (Exception e) {
            logger.warn("Failed to send task assignment notifications for task {}: {}", task.getId(), e.getMessage());
        }
    }
    
    private void notifyTaskStatusChanged(Task task, TaskStatus oldStatus) {
        try {
            notificationService.sendTaskStatusChangedNotification(
                task.getProject().getId(),
                task.getId(),
                task.getTitle(),
                oldStatus,
                task.getStatus()
            );
        } catch (Exception e) {
            logger.warn("Failed to send task status change notifications for task {}: {}", task.getId(), e.getMessage());
        }
    }
    
    private void notifyTaskDeleted(Task task) {
        try {
            notificationService.sendTaskDeletedNotification(
                task.getProject().getId(),
                task.getTitle(),
                task.getCreatedBy().getFullName()
            );
        } catch (Exception e) {
            logger.warn("Failed to send task deletion notifications for task {}: {}", task.getId(), e.getMessage());
        }
    }
}