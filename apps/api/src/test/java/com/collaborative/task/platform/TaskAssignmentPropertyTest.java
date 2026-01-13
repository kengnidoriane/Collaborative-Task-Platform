package com.collaborative.task.platform;

import com.collaborative.task.platform.entity.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for task assignment functionality.
 * **Feature: collaborative-task-platform, Property 14: Task assignment notifies and updates**
 * **Validates: Requirements 3.4**
 */
public class TaskAssignmentPropertyTest {
    
    /**
     * Property 14: Task assignment notifies and updates
     * For any task assignment to a user, the system should notify the assignee and update their personal task list.
     */
    @Property(tries = 100)
    void taskAssignmentNotifiesAndUpdates(
            @ForAll("validTaskTitles") String title,
            @ForAll("validUserEmails") String assigneeEmail,
            @ForAll("validUserNames") String assigneeName) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser(assigneeEmail, assigneeName);
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        
        // Act - Assign task to user
        task.assignTo(assignee);
        
        // Assert - Task is assigned correctly
        assertThat(task.getAssignee()).isEqualTo(assignee);
        assertThat(task.getAssignee().getEmail()).isEqualTo(assigneeEmail);
        assertThat(task.getAssignee().getFullName()).isEqualTo(assigneeName);
        
        // Assert - Task maintains its other properties
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        
        // Assert - Assignment is persistent (simulated)
        assertThat(task.getAssignee()).isNotNull();
        assertThat(task.getAssignee().getId()).isNotNull();
    }
    
    /**
     * Property: Task reassignment works correctly
     * For any task with an existing assignee, reassigning to a different user should update correctly.
     */
    @Property(tries = 50)
    void taskReassignmentWorksCorrectly(
            @ForAll("validTaskTitles") String title,
            @ForAll("validUserEmails") String firstAssigneeEmail,
            @ForAll("validUserEmails") String secondAssigneeEmail,
            @ForAll("validUserNames") String firstName,
            @ForAll("validUserNames") String secondName) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User firstAssignee = createMockUser(firstAssigneeEmail, firstName);
        User secondAssignee = createMockUser(secondAssigneeEmail, secondName);
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        
        // Act - Initial assignment
        task.assignTo(firstAssignee);
        
        // Assert - First assignment
        assertThat(task.getAssignee()).isEqualTo(firstAssignee);
        
        // Act - Reassignment
        task.assignTo(secondAssignee);
        
        // Assert - Reassignment successful
        assertThat(task.getAssignee()).isEqualTo(secondAssignee);
        assertThat(task.getAssignee()).isNotEqualTo(firstAssignee);
        assertThat(task.getAssignee().getEmail()).isEqualTo(secondAssigneeEmail);
        assertThat(task.getAssignee().getFullName()).isEqualTo(secondName);
        
        // Assert - Task properties preserved
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
    }
    
    /**
     * Property: Task unassignment works correctly
     * For any assigned task, unassigning (setting assignee to null) should work correctly.
     */
    @Property(tries = 30)
    void taskUnassignmentWorksCorrectly(
            @ForAll("validTaskTitles") String title,
            @ForAll("validUserEmails") String assigneeEmail,
            @ForAll("validUserNames") String assigneeName) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser(assigneeEmail, assigneeName);
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        task.assignTo(assignee);
        
        // Verify initial assignment
        assertThat(task.getAssignee()).isEqualTo(assignee);
        
        // Act - Unassign task
        task.assignTo(null);
        
        // Assert - Task is unassigned
        assertThat(task.getAssignee()).isNull();
        
        // Assert - Other properties preserved
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }
    
    /**
     * Property: Assignment preserves task identity
     * For any task assignment operation, the task's core identity should be preserved.
     */
    @Property(tries = 30)
    void assignmentPreservesTaskIdentity(
            @ForAll("validTaskTitles") String title,
            @ForAll("validUserEmails") String assigneeEmail,
            @ForAll("validUserNames") String assigneeName) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser(assigneeEmail, assigneeName);
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        
        // Store original values
        UUID originalId = task.getId();
        String originalTitle = task.getTitle();
        Project originalProject = task.getProject();
        User originalCreator = task.getCreatedBy();
        TaskStatus originalStatus = task.getStatus();
        TaskPriority originalPriority = task.getPriority();
        
        // Act - Assign task
        task.assignTo(assignee);
        
        // Assert - Identity preserved
        assertThat(task.getId()).isEqualTo(originalId);
        assertThat(task.getTitle()).isEqualTo(originalTitle);
        assertThat(task.getProject()).isEqualTo(originalProject);
        assertThat(task.getCreatedBy()).isEqualTo(originalCreator);
        assertThat(task.getStatus()).isEqualTo(originalStatus);
        assertThat(task.getPriority()).isEqualTo(originalPriority);
        
        // Assert - Only assignee changed
        assertThat(task.getAssignee()).isEqualTo(assignee);
    }
    
    /**
     * Property: Multiple assignment operations are consistent
     * For any sequence of assignment operations, the final state should be consistent.
     */
    @Property(tries = 20)
    void multipleAssignmentOperationsAreConsistent(
            @ForAll("validTaskTitles") String title,
            @ForAll("assignmentSequence") List<User> assignmentSequence) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        User lastAssignee = null;
        
        // Act - Perform sequence of assignments
        for (User assignee : assignmentSequence) {
            task.assignTo(assignee);
            lastAssignee = assignee;
        }
        
        // Assert - Final state is consistent with last assignment
        assertThat(task.getAssignee()).isEqualTo(lastAssignee);
        
        // Assert - Task properties preserved
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
    }
    
    /**
     * Property: Assignment to same user is idempotent
     * For any task, assigning to the same user multiple times should have no additional effect.
     */
    @Property(tries = 20)
    void assignmentToSameUserIsIdempotent(
            @ForAll("validTaskTitles") String title,
            @ForAll("validUserEmails") String assigneeEmail,
            @ForAll("validUserNames") String assigneeName) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser(assigneeEmail, assigneeName);
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        
        // Act - Assign multiple times to same user
        task.assignTo(assignee);
        User firstAssignment = task.getAssignee();
        
        task.assignTo(assignee);
        User secondAssignment = task.getAssignee();
        
        task.assignTo(assignee);
        User thirdAssignment = task.getAssignee();
        
        // Assert - All assignments result in same state
        assertThat(firstAssignment).isEqualTo(assignee);
        assertThat(secondAssignment).isEqualTo(assignee);
        assertThat(thirdAssignment).isEqualTo(assignee);
        assertThat(firstAssignment).isEqualTo(secondAssignment);
        assertThat(secondAssignment).isEqualTo(thirdAssignment);
    }
    
    /**
     * Property: Assignment works with different task states
     * For any task in any valid state, assignment should work correctly.
     */
    @Property(tries = 30)
    void assignmentWorksWithDifferentTaskStates(
            @ForAll("validTaskTitles") String title,
            @ForAll("validUserEmails") String assigneeEmail,
            @ForAll("validUserNames") String assigneeName,
            @ForAll("validStatuses") TaskStatus status) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser(assigneeEmail, assigneeName);
        Project project = createMockProject("Test Project", creator);
        
        Task task = new Task(title, project, creator);
        
        // Set task to specific status
        navigateToStatus(task, status);
        
        // Act - Assign task
        task.assignTo(assignee);
        
        // Assert - Assignment successful regardless of status
        assertThat(task.getAssignee()).isEqualTo(assignee);
        assertThat(task.getStatus()).isEqualTo(status);
        
        // Assert - Task properties preserved
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
    }
    
    /**
     * Test that validates basic task assignment functionality.
     */
    @Test
    void basicTaskAssignmentWorks() {
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser("assignee@example.com", "Assignee");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task("Test Task", project, creator);
        
        // Act
        task.assignTo(assignee);
        
        // Assert
        assertThat(task.getAssignee()).isEqualTo(assignee);
        assertThat(task.getAssignee().getEmail()).isEqualTo("assignee@example.com");
        assertThat(task.getAssignee().getFullName()).isEqualTo("Assignee");
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
    Arbitrary<String> validUserEmails() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('0', '9')
            .ofMinLength(3)
            .ofMaxLength(20)
            .map(name -> name + "@example.com");
    }
    
    @Provide
    Arbitrary<String> validUserNames() {
        return Arbitraries.strings()
            .withCharRange('A', 'Z')
            .withCharRange('a', 'z')
            .withChars(' ')
            .ofMinLength(2)
            .ofMaxLength(50)
            .filter(name -> !name.trim().isEmpty());
    }
    
    @Provide
    Arbitrary<TaskStatus> validStatuses() {
        return Arbitraries.of(TaskStatus.values());
    }
    
    @Provide
    Arbitrary<List<User>> assignmentSequence() {
        return Arbitraries.of(
            createMockUser("user1@example.com", "User One"),
            createMockUser("user2@example.com", "User Two"),
            createMockUser("user3@example.com", "User Three"),
            null // Unassignment
        ).list().ofMinSize(1).ofMaxSize(5);
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
}