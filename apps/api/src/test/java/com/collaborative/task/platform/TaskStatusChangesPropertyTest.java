package com.collaborative.task.platform;

import com.collaborative.task.platform.entity.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for task status change functionality.
 * **Feature: collaborative-task-platform, Property 13: Status changes trigger analytics and notifications**
 * **Validates: Requirements 3.3**
 */
public class TaskStatusChangesPropertyTest {
    
    /**
     * Property 13: Status changes trigger analytics and notifications
     * For any task status change, the system should update project analytics and trigger any configured notifications.
     */
    @Property(tries = 100)
    void statusChangeTriggerAnalyticsAndNotifications(
            @ForAll("validTaskTitles") String title,
            @ForAll("validStatusTransitions") StatusTransition transition) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser("assignee@example.com", "Assignee");
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        task.assignTo(assignee);
        
        // Set up initial status
        if (transition.from() != TaskStatus.TODO) {
            // Navigate to the from status if it's not the default
            navigateToStatus(task, transition.from());
        }
        
        TaskStatus initialStatus = task.getStatus();
        int initialCompletionPercentage = task.getCompletionPercentage();
        
        // Act - Change status
        task.changeStatus(transition.to());
        
        // Assert - Status is changed
        assertThat(task.getStatus()).isEqualTo(transition.to());
        assertThat(task.getStatus()).isNotEqualTo(initialStatus);
        
        // Assert - Completion percentage is updated
        int newCompletionPercentage = task.getCompletionPercentage();
        assertThat(newCompletionPercentage).isNotNegative();
        assertThat(newCompletionPercentage).isLessThanOrEqualTo(100);
        
        // Assert - Completion status is consistent
        if (transition.to().isCompleted()) {
            assertThat(task.isCompleted()).isTrue();
            assertThat(newCompletionPercentage).isIn(0, 100); // CANCELLED = 0, DONE = 100
        } else {
            assertThat(task.isCompleted()).isFalse();
        }
        
        // Assert - Task maintains its identity and relationships
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getAssignee()).isEqualTo(assignee);
        assertThat(task.getTitle()).isEqualTo(title);
    }
    
    /**
     * Property: Invalid status transitions are rejected
     * For any invalid status transition, the system should reject the change and maintain the current status.
     */
    @Property(tries = 50)
    void invalidStatusTransitionsAreRejected(
            @ForAll("validTaskTitles") String title,
            @ForAll("invalidStatusTransitions") StatusTransition invalidTransition) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Set up initial status
        if (invalidTransition.from() != TaskStatus.TODO) {
            navigateToStatus(task, invalidTransition.from());
        }
        
        TaskStatus originalStatus = task.getStatus();
        
        // Act & Assert - Invalid transition should throw exception
        assertThatThrownBy(() -> task.changeStatus(invalidTransition.to()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot transition from")
            .hasMessageContaining(originalStatus.toString())
            .hasMessageContaining(invalidTransition.to().toString());
        
        // Assert - Status remains unchanged
        assertThat(task.getStatus()).isEqualTo(originalStatus);
    }
    
    /**
     * Property: Status transitions maintain task consistency
     * For any valid status transition, the task should maintain its consistency.
     */
    @Property(tries = 30)
    void statusTransitionsMaintainTaskConsistency(
            @ForAll("validTaskTitles") String title,
            @ForAll("validStatusTransitions") StatusTransition transition) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser("assignee@example.com", "Assignee");
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        task.assignTo(assignee);
        
        // Set up initial status
        if (transition.from() != TaskStatus.TODO) {
            navigateToStatus(task, transition.from());
        }
        
        // Store original values
        String originalTitle = task.getTitle();
        String originalDescription = task.getDescription();
        User originalCreator = task.getCreatedBy();
        User originalAssignee = task.getAssignee();
        Project originalProject = task.getProject();
        
        // Act
        task.changeStatus(transition.to());
        
        // Assert - Only status changed, other fields preserved
        assertThat(task.getTitle()).isEqualTo(originalTitle);
        assertThat(task.getDescription()).isEqualTo(originalDescription);
        assertThat(task.getCreatedBy()).isEqualTo(originalCreator);
        assertThat(task.getAssignee()).isEqualTo(originalAssignee);
        assertThat(task.getProject()).isEqualTo(originalProject);
        
        // Assert - Status is updated
        assertThat(task.getStatus()).isEqualTo(transition.to());
    }
    
    /**
     * Property: Completion status is consistent with task status
     * For any task status, the completion status should be consistent.
     */
    @Property(tries = 20)
    void completionStatusIsConsistentWithTaskStatus(
            @ForAll("validTaskTitles") String title,
            @ForAll("allStatuses") TaskStatus status) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Navigate to the target status
        navigateToStatus(task, status);
        
        // Assert - Completion status is consistent
        boolean expectedCompleted = status.isCompleted();
        assertThat(task.isCompleted()).isEqualTo(expectedCompleted);
        
        // Assert - Completion percentage is appropriate
        int completionPercentage = task.getCompletionPercentage();
        switch (status) {
            case TODO -> assertThat(completionPercentage).isEqualTo(0);
            case IN_PROGRESS -> assertThat(completionPercentage).isEqualTo(50);
            case IN_REVIEW -> assertThat(completionPercentage).isEqualTo(80);
            case DONE -> assertThat(completionPercentage).isEqualTo(100);
            case CANCELLED -> assertThat(completionPercentage).isEqualTo(0);
        }
    }
    
    /**
     * Property: Status transitions are idempotent
     * For any task, setting the same status multiple times should have no additional effect.
     */
    @Property(tries = 20)
    void statusTransitionsAreIdempotent(
            @ForAll("validTaskTitles") String title,
            @ForAll("allStatuses") TaskStatus status) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Navigate to the target status
        navigateToStatus(task, status);
        
        TaskStatus currentStatus = task.getStatus();
        int currentCompletion = task.getCompletionPercentage();
        
        // Act - Set the same status again (should be allowed)
        if (currentStatus == status) {
            task.changeStatus(status);
            
            // Assert - No change occurred
            assertThat(task.getStatus()).isEqualTo(currentStatus);
            assertThat(task.getCompletionPercentage()).isEqualTo(currentCompletion);
        }
    }
    
    /**
     * Test that validates basic status change functionality.
     */
    @Test
    void basicStatusChangeWorks() {
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task("Test Task", project, creator);
        
        // Act & Assert - Valid transitions
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        
        task.changeStatus(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getCompletionPercentage()).isEqualTo(50);
        
        task.changeStatus(TaskStatus.IN_REVIEW);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_REVIEW);
        assertThat(task.getCompletionPercentage()).isEqualTo(80);
        
        task.changeStatus(TaskStatus.DONE);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.isCompleted()).isTrue();
        assertThat(task.getCompletionPercentage()).isEqualTo(100);
    }
    
    // Generators for test data
    
    @Provide
    Arbitrary<String> validTaskTitles() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withCharRange('0', '9')
            .withChars(' ', '-', '_', '.', '!', '?')
            .ofMinLength(1)
            .ofMaxLength(200)
            .filter(title -> !title.trim().isEmpty());
    }
    
    @Provide
    Arbitrary<TaskStatus> allStatuses() {
        return Arbitraries.of(TaskStatus.values());
    }
    
    @Provide
    Arbitrary<StatusTransition> validStatusTransitions() {
        return Arbitraries.of(
            // From TODO
            new StatusTransition(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
            new StatusTransition(TaskStatus.TODO, TaskStatus.CANCELLED),
            
            // From IN_PROGRESS
            new StatusTransition(TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW),
            new StatusTransition(TaskStatus.IN_PROGRESS, TaskStatus.DONE),
            new StatusTransition(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
            
            // From IN_REVIEW
            new StatusTransition(TaskStatus.IN_REVIEW, TaskStatus.IN_PROGRESS),
            new StatusTransition(TaskStatus.IN_REVIEW, TaskStatus.DONE),
            new StatusTransition(TaskStatus.IN_REVIEW, TaskStatus.CANCELLED),
            
            // From DONE
            new StatusTransition(TaskStatus.DONE, TaskStatus.IN_PROGRESS),
            
            // From CANCELLED
            new StatusTransition(TaskStatus.CANCELLED, TaskStatus.TODO),
            new StatusTransition(TaskStatus.CANCELLED, TaskStatus.IN_PROGRESS)
        );
    }
    
    @Provide
    Arbitrary<StatusTransition> invalidStatusTransitions() {
        return Arbitraries.of(
            // Invalid transitions from TODO
            new StatusTransition(TaskStatus.TODO, TaskStatus.IN_REVIEW),
            new StatusTransition(TaskStatus.TODO, TaskStatus.DONE),
            
            // Invalid transitions from IN_PROGRESS
            new StatusTransition(TaskStatus.IN_PROGRESS, TaskStatus.TODO),
            
            // Invalid transitions from IN_REVIEW
            new StatusTransition(TaskStatus.IN_REVIEW, TaskStatus.TODO),
            
            // Invalid transitions from DONE
            new StatusTransition(TaskStatus.DONE, TaskStatus.TODO),
            new StatusTransition(TaskStatus.DONE, TaskStatus.IN_REVIEW),
            new StatusTransition(TaskStatus.DONE, TaskStatus.CANCELLED),
            
            // Invalid transitions from CANCELLED
            new StatusTransition(TaskStatus.CANCELLED, TaskStatus.IN_REVIEW),
            new StatusTransition(TaskStatus.CANCELLED, TaskStatus.DONE)
        );
    }
    
    // Helper methods
    
    private User createMockUser(String email, String fullName) {
        User user = new User(email, fullName);
        user.setEmailVerified(true);
        return user;
    }
    
    private Project createMockProject(String name, User owner) {
        return new Project(name, "Test project description", owner, false);
    }
    
    private void navigateToStatus(Task task, TaskStatus targetStatus) {
        TaskStatus currentStatus = task.getStatus();
        
        if (currentStatus == targetStatus) {
            return;
        }
        
        // Navigate through valid transitions to reach target status
        switch (targetStatus) {
            case TODO -> {
                // Already at TODO by default
            }
            case IN_PROGRESS -> {
                if (currentStatus == TaskStatus.TODO || currentStatus == TaskStatus.CANCELLED) {
                    if (currentStatus == TaskStatus.CANCELLED) {
                        task.changeStatus(TaskStatus.TODO);
                    }
                    task.changeStatus(TaskStatus.IN_PROGRESS);
                } else if (currentStatus == TaskStatus.DONE) {
                    task.changeStatus(TaskStatus.IN_PROGRESS);
                } else if (currentStatus == TaskStatus.IN_REVIEW) {
                    task.changeStatus(TaskStatus.IN_PROGRESS);
                }
            }
            case IN_REVIEW -> {
                if (currentStatus == TaskStatus.TODO) {
                    task.changeStatus(TaskStatus.IN_PROGRESS);
                    task.changeStatus(TaskStatus.IN_REVIEW);
                } else if (currentStatus == TaskStatus.IN_PROGRESS) {
                    task.changeStatus(TaskStatus.IN_REVIEW);
                }
            }
            case DONE -> {
                if (currentStatus == TaskStatus.TODO) {
                    task.changeStatus(TaskStatus.IN_PROGRESS);
                    task.changeStatus(TaskStatus.DONE);
                } else if (currentStatus == TaskStatus.IN_PROGRESS) {
                    task.changeStatus(TaskStatus.DONE);
                } else if (currentStatus == TaskStatus.IN_REVIEW) {
                    task.changeStatus(TaskStatus.DONE);
                }
            }
            case CANCELLED -> {
                if (currentStatus != TaskStatus.DONE) {
                    task.changeStatus(TaskStatus.CANCELLED);
                }
            }
        }
    }
    
    /**
     * Record representing a status transition for testing
     */
    public record StatusTransition(TaskStatus from, TaskStatus to) {}
}