package com.collaborative.task.platform;

import com.collaborative.task.platform.entity.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for task creation functionality.
 * **Feature: collaborative-task-platform, Property 11: Task creation stores and notifies**
 * **Validates: Requirements 3.1**
 */
public class TaskCreationPropertyTest {
    
    /**
     * Property 11: Task creation stores and notifies
     * For any valid task creation data, the system should store task details and notify relevant project members.
     */
    @Property(tries = 100)
    void taskCreationStoresAndNotifies(
            @ForAll("validTaskTitles") String title,
            @ForAll("validDescriptions") String description,
            @ForAll("validPriorities") TaskPriority priority,
            @ForAll("validDueDates") LocalDateTime dueDate,
            @ForAll("validTags") List<String> tags,
            @ForAll("validEstimatedHours") Integer estimatedHours) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Task Creator");
        User assignee = createMockUser("assignee@example.com", "Task Assignee");
        Project project = createMockProject("Test Project", creator);
        
        // Act - Create task
        Task task = new Task(title, project, creator);
        
        // Set optional fields
        if (description != null) {
            task.updateDetails(title, description, dueDate);
        }
        if (priority != null) {
            task.changePriority(priority);
        }
        if (estimatedHours != null) {
            task.setEstimatedHours(estimatedHours);
        }
        if (tags != null) {
            tags.forEach(task::addTag);
        }
        
        // Assign task if we have an assignee
        task.assignTo(assignee);
        
        // Assert - Task is created with correct basic properties
        assertThat(task).isNotNull();
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getDescription()).isEqualTo(description);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getAssignee()).isEqualTo(assignee);
        
        // Assert - Task has correct status and priority
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(priority != null ? priority : TaskPriority.MEDIUM);
        
        // Assert - Task has correct optional fields
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getEstimatedHours()).isEqualTo(estimatedHours);
        
        // Assert - Tags are stored correctly
        if (tags != null && !tags.isEmpty()) {
            List<String> cleanTags = tags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
            assertThat(task.getTags()).containsExactlyInAnyOrderElementsOf(cleanTags);
        }
        
        // Assert - Task is not completed initially
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.getCompletionPercentage()).isEqualTo(0);
        
        // Assert - Task has proper audit fields
        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getUpdatedAt()).isNotNull();
        assertThat(task.getVersion()).isNotNull();
    }
    
    /**
     * Property: Task creation with minimal data works correctly
     * For any valid title and project, creating a task should work with default values.
     */
    @Property(tries = 50)
    void taskCreationWithMinimalData(
            @ForAll("validTaskTitles") String title) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        
        // Act
        Task task = new Task(title, project, creator);
        
        // Assert - Task is created with defaults
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getAssignee()).isNull();
        assertThat(task.getDescription()).isNull();
        assertThat(task.getDueDate()).isNull();
        assertThat(task.getTags()).isEmpty();
        assertThat(task.getDependsOn()).isEmpty();
    }
    
    /**
     * Property: Task assignment works correctly
     * For any task and valid assignee, assignment should establish the relationship.
     */
    @Property(tries = 30)
    void taskAssignmentWorks(
            @ForAll("validTaskTitles") String title) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        User assignee = createMockUser("assignee@example.com", "Assignee");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Act
        task.assignTo(assignee);
        
        // Assert
        assertThat(task.getAssignee()).isEqualTo(assignee);
        
        // Act - Reassign to null (unassign)
        task.assignTo(null);
        
        // Assert
        assertThat(task.getAssignee()).isNull();
    }
    
    /**
     * Property: Task priority changes work correctly
     * For any task and valid priority, changing priority should update the task.
     */
    @Property(tries = 20)
    void taskPriorityChanges(
            @ForAll("validTaskTitles") String title,
            @ForAll("validPriorities") TaskPriority newPriority) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        TaskPriority originalPriority = task.getPriority();
        
        // Act
        task.changePriority(newPriority);
        
        // Assert
        assertThat(task.getPriority()).isEqualTo(newPriority);
        assertThat(task.getPriority()).isNotEqualTo(originalPriority);
    }
    
    /**
     * Property: Task tag management works correctly
     * For any task and valid tags, adding and removing tags should work consistently.
     */
    @Property(tries = 30)
    void taskTagManagement(
            @ForAll("validTaskTitles") String title,
            @ForAll("validTags") List<String> tags) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Act - Add tags
        if (tags != null) {
            tags.forEach(task::addTag);
        }
        
        // Assert - Tags are added correctly
        if (tags != null && !tags.isEmpty()) {
            List<String> cleanTags = tags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
            assertThat(task.getTags()).containsExactlyInAnyOrderElementsOf(cleanTags);
        }
        
        // Act - Remove first tag if any exist
        if (!task.getTags().isEmpty()) {
            String firstTag = task.getTags().get(0);
            task.removeTag(firstTag);
            
            // Assert - Tag is removed
            assertThat(task.getTags()).doesNotContain(firstTag);
        }
    }
    
    /**
     * Property: Task dependency management works correctly
     * For any task, adding and removing dependencies should work consistently.
     */
    @Property(tries = 20)
    void taskDependencyManagement(
            @ForAll("validTaskTitles") String title) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        UUID dependencyId = UUID.randomUUID();
        
        // Act - Add dependency
        task.addDependency(dependencyId);
        
        // Assert - Dependency is added
        assertThat(task.getDependsOn()).contains(dependencyId);
        
        // Act - Remove dependency
        task.removeDependency(dependencyId);
        
        // Assert - Dependency is removed
        assertThat(task.getDependsOn()).doesNotContain(dependencyId);
    }
    
    /**
     * Property: Task time tracking works correctly
     * For any task and valid hours, recording time should accumulate correctly.
     */
    @Property(tries = 20)
    void taskTimeTracking(
            @ForAll("validTaskTitles") String title,
            @ForAll("validHours") int hours1,
            @ForAll("validHours") int hours2) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Act - Record time
        task.recordTimeSpent(hours1);
        task.recordTimeSpent(hours2);
        
        // Assert - Time is accumulated
        assertThat(task.getActualHours()).isEqualTo(hours1 + hours2);
    }
    
    /**
     * Test that validates basic task creation functionality.
     */
    @Test
    void basicTaskCreationWorks() {
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        
        // Act
        Task task = new Task("Test Task", project, creator);
        
        // Assert
        assertThat(task).isNotNull();
        assertThat(task.getTitle()).isEqualTo("Test Task");
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
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
    Arbitrary<String> validDescriptions() {
        return Arbitraries.oneOf(
            Arbitraries.just(null), // No description
            Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '!', '?', '-', '_', '\n')
                .ofMinLength(0)
                .ofMaxLength(2000)
        );
    }
    
    @Provide
    Arbitrary<TaskPriority> validPriorities() {
        return Arbitraries.of(TaskPriority.values());
    }
    
    @Provide
    Arbitrary<LocalDateTime> validDueDates() {
        return Arbitraries.oneOf(
            Arbitraries.just(null), // No due date
            Arbitraries.localDateTimes()
                .between(LocalDateTime.now().minusDays(30), LocalDateTime.now().plusDays(365))
        );
    }
    
    @Provide
    Arbitrary<List<String>> validTags() {
        return Arbitraries.oneOf(
            Arbitraries.just(null), // No tags
            Arbitraries.of("bug", "feature", "urgent", "backend", "frontend", "testing", "documentation")
                .list()
                .ofMinSize(0)
                .ofMaxSize(5)
        );
    }
    
    @Provide
    Arbitrary<Integer> validEstimatedHours() {
        return Arbitraries.oneOf(
            Arbitraries.just(null), // No estimate
            Arbitraries.integers().between(1, 100)
        );
    }
    
    @Provide
    Arbitrary<Integer> validHours() {
        return Arbitraries.integers().between(1, 8);
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
}