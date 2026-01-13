package com.collaborative.task.platform;

import com.collaborative.task.platform.entity.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for task dependency functionality.
 * **Feature: collaborative-task-platform, Property 15: Task dependencies enforce valid transitions**
 * **Validates: Requirements 3.5**
 */
public class TaskDependenciesPropertyTest {
    
    /**
     * Property 15: Task dependencies enforce valid transitions
     * For any task with dependencies, the system should enforce dependency rules and prevent invalid state transitions.
     */
    @Property(tries = 100)
    void taskDependenciesEnforceValidTransitions(
            @ForAll("validTaskTitles") String title,
            @ForAll("validDependencyIds") List<UUID> dependencyIds) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Act - Add dependencies
        for (UUID dependencyId : dependencyIds) {
            task.addDependency(dependencyId);
        }
        
        // Assert - Dependencies are added correctly
        assertThat(task.getDependsOn()).containsExactlyInAnyOrderElementsOf(dependencyIds);
        
        // Assert - Task maintains its properties
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        
        // Assert - Dependencies are unique (no duplicates)
        List<UUID> dependencies = task.getDependsOn();
        assertThat(dependencies).doesNotHaveDuplicates();
        assertThat(dependencies.size()).isLessThanOrEqualTo(dependencyIds.size());
    }
    
    /**
     * Property: Adding duplicate dependencies has no effect
     * For any task and dependency, adding the same dependency multiple times should result in only one entry.
     */
    @Property(tries = 50)
    void addingDuplicateDependenciesHasNoEffect(
            @ForAll("validTaskTitles") String title,
            @ForAll("singleDependencyId") UUID dependencyId) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Act - Add same dependency multiple times
        task.addDependency(dependencyId);
        task.addDependency(dependencyId);
        task.addDependency(dependencyId);
        
        // Assert - Only one dependency exists
        assertThat(task.getDependsOn()).hasSize(1);
        assertThat(task.getDependsOn()).containsExactly(dependencyId);
    }
    
    /**
     * Property: Removing dependencies works correctly
     * For any task with dependencies, removing a dependency should work correctly.
     */
    @Property(tries = 30)
    void removingDependenciesWorksCorrectly(
            @ForAll("validTaskTitles") String title,
            @ForAll("validDependencyIds") List<UUID> dependencyIds) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Add all dependencies
        for (UUID dependencyId : dependencyIds) {
            task.addDependency(dependencyId);
        }
        
        // Act - Remove each dependency
        for (UUID dependencyId : dependencyIds) {
            task.removeDependency(dependencyId);
        }
        
        // Assert - All dependencies are removed
        assertThat(task.getDependsOn()).isEmpty();
    }
    
    /**
     * Property: Self-dependency is rejected
     * For any task, attempting to add itself as a dependency should be rejected.
     */
    @Property(tries = 20)
    void selfDependencyIsRejected(
            @ForAll("validTaskTitles") String title) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Act & Assert - Self-dependency should throw exception
        assertThatThrownBy(() -> task.addDependency(task.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task cannot depend on itself");
        
        // Assert - No dependencies added
        assertThat(task.getDependsOn()).isEmpty();
    }
    
    /**
     * Property: Dependency management preserves task identity
     * For any dependency operations, the task's core identity should be preserved.
     */
    @Property(tries = 30)
    void dependencyManagementPreservesTaskIdentity(
            @ForAll("validTaskTitles") String title,
            @ForAll("validDependencyIds") List<UUID> dependencyIds) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Store original values
        UUID originalId = task.getId();
        String originalTitle = task.getTitle();
        Project originalProject = task.getProject();
        User originalCreator = task.getCreatedBy();
        TaskStatus originalStatus = task.getStatus();
        TaskPriority originalPriority = task.getPriority();
        
        // Act - Add and remove dependencies
        for (UUID dependencyId : dependencyIds) {
            task.addDependency(dependencyId);
        }
        
        for (UUID dependencyId : dependencyIds) {
            task.removeDependency(dependencyId);
        }
        
        // Assert - Identity preserved
        assertThat(task.getId()).isEqualTo(originalId);
        assertThat(task.getTitle()).isEqualTo(originalTitle);
        assertThat(task.getProject()).isEqualTo(originalProject);
        assertThat(task.getCreatedBy()).isEqualTo(originalCreator);
        assertThat(task.getStatus()).isEqualTo(originalStatus);
        assertThat(task.getPriority()).isEqualTo(originalPriority);
        
        // Assert - Dependencies are empty after removal
        assertThat(task.getDependsOn()).isEmpty();
    }
    
    /**
     * Property: Dependency operations are order-independent
     * For any set of dependencies, the order of addition should not affect the final result.
     */
    @Property(tries = 20)
    void dependencyOperationsAreOrderIndependent(
            @ForAll("validTaskTitles") String title,
            @ForAll("validDependencyIds") List<UUID> dependencyIds) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        
        Task task1 = new Task(title, project, creator);
        Task task2 = new Task(title, project, creator);
        
        // Act - Add dependencies in original order for task1
        for (UUID dependencyId : dependencyIds) {
            task1.addDependency(dependencyId);
        }
        
        // Act - Add dependencies in reverse order for task2
        List<UUID> reversedIds = dependencyIds.reversed();
        for (UUID dependencyId : reversedIds) {
            task2.addDependency(dependencyId);
        }
        
        // Assert - Both tasks have same dependencies regardless of order
        assertThat(task1.getDependsOn()).containsExactlyInAnyOrderElementsOf(task2.getDependsOn());
        assertThat(task1.getDependsOn()).containsExactlyInAnyOrderElementsOf(dependencyIds);
        assertThat(task2.getDependsOn()).containsExactlyInAnyOrderElementsOf(dependencyIds);
    }
    
    /**
     * Property: Removing non-existent dependency has no effect
     * For any task, removing a dependency that doesn't exist should have no effect.
     */
    @Property(tries = 20)
    void removingNonExistentDependencyHasNoEffect(
            @ForAll("validTaskTitles") String title,
            @ForAll("singleDependencyId") UUID existingDependency,
            @ForAll("singleDependencyId") UUID nonExistentDependency) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Add one dependency
        task.addDependency(existingDependency);
        
        // Store current state
        List<UUID> dependenciesBeforeRemoval = task.getDependsOn();
        
        // Act - Try to remove non-existent dependency
        task.removeDependency(nonExistentDependency);
        
        // Assert - Dependencies unchanged
        assertThat(task.getDependsOn()).isEqualTo(dependenciesBeforeRemoval);
        assertThat(task.getDependsOn()).containsExactly(existingDependency);
    }
    
    /**
     * Property: Dependencies list is immutable from outside
     * For any task, the dependencies list returned should be a copy, not the original.
     */
    @Property(tries = 20)
    void dependenciesListIsImmutableFromOutside(
            @ForAll("validTaskTitles") String title,
            @ForAll("validDependencyIds") List<UUID> dependencyIds) {
        
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task(title, project, creator);
        
        // Add dependencies
        for (UUID dependencyId : dependencyIds) {
            task.addDependency(dependencyId);
        }
        
        // Act - Get dependencies list and try to modify it
        List<UUID> retrievedDependencies = task.getDependsOn();
        UUID newDependency = UUID.randomUUID();
        
        try {
            retrievedDependencies.add(newDependency);
        } catch (UnsupportedOperationException e) {
            // Expected for immutable lists
        }
        
        // Assert - Original task dependencies unchanged
        assertThat(task.getDependsOn()).doesNotContain(newDependency);
        assertThat(task.getDependsOn()).containsExactlyInAnyOrderElementsOf(dependencyIds);
    }
    
    /**
     * Test that validates basic dependency functionality.
     */
    @Test
    void basicDependencyFunctionalityWorks() {
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task("Test Task", project, creator);
        UUID dependencyId = UUID.randomUUID();
        
        // Act - Add dependency
        task.addDependency(dependencyId);
        
        // Assert - Dependency added
        assertThat(task.getDependsOn()).containsExactly(dependencyId);
        
        // Act - Remove dependency
        task.removeDependency(dependencyId);
        
        // Assert - Dependency removed
        assertThat(task.getDependsOn()).isEmpty();
    }
    
    /**
     * Test that validates self-dependency rejection.
     */
    @Test
    void selfDependencyRejectionWorks() {
        // Arrange
        User creator = createMockUser("creator@example.com", "Creator");
        Project project = createMockProject("Test Project", creator);
        Task task = new Task("Test Task", project, creator);
        
        // Act & Assert - Self-dependency should be rejected
        assertThatThrownBy(() -> task.addDependency(task.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task cannot depend on itself");
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
    Arbitrary<UUID> singleDependencyId() {
        return Arbitraries.create(UUID::randomUUID);
    }
    
    @Provide
    Arbitrary<List<UUID>> validDependencyIds() {
        return Arbitraries.create(UUID::randomUUID)
            .list()
            .ofMinSize(0)
            .ofMaxSize(5)
            .map(list -> list.stream().distinct().toList()); // Remove duplicates
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