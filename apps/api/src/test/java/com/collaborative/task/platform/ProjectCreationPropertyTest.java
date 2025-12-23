package com.collaborative.task.platform;

import com.collaborative.task.platform.dto.project.CreateProjectRequest;
import com.collaborative.task.platform.entity.Project;
import com.collaborative.task.platform.entity.ProjectRole;
import com.collaborative.task.platform.entity.User;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for project creation functionality.
 * **Feature: collaborative-task-platform, Property 6: Project creation establishes ownership**
 * **Validates: Requirements 2.1**
 */
public class ProjectCreationPropertyTest {
    
    /**
     * Property 6: Project creation establishes ownership
     * For any valid project creation request, the system should establish the user as project owner with full permissions.
     */
    @Property(tries = 100)
    void projectCreationEstablishesOwnership(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validDescriptions") String description,
            @ForAll("validPrivacySettings") boolean isPrivate) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Project Owner");
        
        // Act - Create project directly (simulating service layer logic)
        Project project = new Project(projectName, description, owner, isPrivate);
        
        // Assert - Project is created with correct ownership
        assertThat(project).isNotNull();
        assertThat(project.getName()).isEqualTo(projectName);
        assertThat(project.getDescription()).isEqualTo(description);
        assertThat(project.isPrivate()).isEqualTo(isPrivate);
        assertThat(project.getOwner()).isEqualTo(owner);
        assertThat(project.isArchived()).isFalse();
        
        // Assert - Owner has full permissions
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.hasAdminAccess(owner)).isTrue();
        assertThat(project.getMemberRole(owner)).isEqualTo(ProjectRole.OWNER);
        assertThat(project.isMember(owner)).isTrue();
        assertThat(project.getMemberCount()).isEqualTo(1); // Owner is automatically a member
        
        // Assert - Project is active by default
        assertThat(project.isActive()).isTrue();
    }
    
    /**
     * Property: Project creation with valid data produces consistent results
     * For any valid project data, creating multiple projects should produce consistent ownership patterns.
     */
    @Property(tries = 50)
    void projectCreationConsistency(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validDescriptions") String description) {
        
        // Arrange
        User owner1 = createMockUser("owner1@example.com", "Owner One");
        User owner2 = createMockUser("owner2@example.com", "Owner Two");
        
        // Act - Create two projects with same data but different owners
        Project project1 = new Project(projectName, description, owner1, false);
        Project project2 = new Project(projectName, description, owner2, false);
        
        // Assert - Both projects have correct ownership
        assertThat(project1.isOwner(owner1)).isTrue();
        assertThat(project1.isOwner(owner2)).isFalse();
        assertThat(project2.isOwner(owner2)).isTrue();
        assertThat(project2.isOwner(owner1)).isFalse();
        
        // Assert - Projects are independent
        assertThat(project1.equals(project2)).isFalse();
        assertThat(project1.getName()).isEqualTo(project2.getName());
        assertThat(project1.getDescription()).isEqualTo(project2.getDescription());
    }
    
    /**
     * Property: Project member management works correctly
     * For any project, adding and removing members should work consistently.
     */
    @Property(tries = 30)
    void projectMemberManagement(
            @ForAll("validProjectNames") String projectName) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Project Owner");
        User member = createMockUser("member@example.com", "Team Member");
        Project project = new Project(projectName, "Description", owner, false);
        
        // Act & Assert - Add member
        project.addMember(member, ProjectRole.MEMBER);
        assertThat(project.isMember(member)).isTrue();
        assertThat(project.getMemberRole(member)).isEqualTo(ProjectRole.MEMBER);
        assertThat(project.getMemberCount()).isEqualTo(2); // Owner + member
        
        // Act & Assert - Remove member
        project.removeMember(member);
        assertThat(project.isMember(member)).isFalse();
        assertThat(project.getMemberRole(member)).isNull();
        assertThat(project.getMemberCount()).isEqualTo(1); // Only owner remains
        
        // Assert - Owner cannot be removed through member management
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.isMember(owner)).isTrue();
    }
    
    /**
     * Property: Project role hierarchy works correctly
     * For any project, role permissions should follow the correct hierarchy.
     */
    @Property(tries = 20)
    void projectRoleHierarchy(@ForAll("validProjectNames") String projectName) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User admin = createMockUser("admin@example.com", "Admin");
        User member = createMockUser("member@example.com", "Member");
        User viewer = createMockUser("viewer@example.com", "Viewer");
        
        Project project = new Project(projectName, "Description", owner, false);
        project.addMember(admin, ProjectRole.ADMIN);
        project.addMember(member, ProjectRole.MEMBER);
        project.addMember(viewer, ProjectRole.VIEWER);
        
        // Assert - Owner has highest privileges
        assertThat(project.hasAdminAccess(owner)).isTrue();
        assertThat(project.getMemberRole(owner)).isEqualTo(ProjectRole.OWNER);
        
        // Assert - Admin has admin privileges
        assertThat(project.hasAdminAccess(admin)).isTrue();
        assertThat(project.getMemberRole(admin)).isEqualTo(ProjectRole.ADMIN);
        
        // Assert - Member has no admin privileges
        assertThat(project.hasAdminAccess(member)).isFalse();
        assertThat(project.getMemberRole(member)).isEqualTo(ProjectRole.MEMBER);
        
        // Assert - Viewer has no admin privileges
        assertThat(project.hasAdminAccess(viewer)).isFalse();
        assertThat(project.getMemberRole(viewer)).isEqualTo(ProjectRole.VIEWER);
        
        // Assert - All are members
        assertThat(project.isMember(owner)).isTrue();
        assertThat(project.isMember(admin)).isTrue();
        assertThat(project.isMember(member)).isTrue();
        assertThat(project.isMember(viewer)).isTrue();
    }
    
    /**
     * Test that validates basic project creation functionality.
     */
    @Test
    void basicProjectCreationWorks() {
        // Arrange
        User owner = createMockUser("test@example.com", "Test User");
        
        // Act
        Project project = new Project("Test Project", "Test Description", owner, false);
        
        // Assert
        assertThat(project).isNotNull();
        assertThat(project.getName()).isEqualTo("Test Project");
        assertThat(project.getDescription()).isEqualTo("Test Description");
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.isPrivate()).isFalse();
        assertThat(project.isArchived()).isFalse();
    }
    
    /**
     * Test that validates CreateProjectRequest creation.
     */
    @Test
    void createProjectRequestValidation() {
        // Arrange & Act
        CreateProjectRequest request = new CreateProjectRequest("Test Project", "Description", true);
        
        // Assert
        assertThat(request.name()).isEqualTo("Test Project");
        assertThat(request.description()).isEqualTo("Description");
        assertThat(request.isPrivate()).isTrue();
    }
    
    // Generators for test data
    
    @Provide
    Arbitrary<String> validProjectNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withCharRange('0', '9')
            .withChars(' ', '-', '_')
            .ofMinLength(1)
            .ofMaxLength(100)
            .filter(name -> !name.trim().isEmpty());
    }
    
    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.oneOf(
            Arbitraries.just(null), // No description
            Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '!', '?', '-', '_')
                .ofMinLength(0)
                .ofMaxLength(500)
        );
    }
    
    @Provide
    Arbitrary<Boolean> validPrivacySettings() {
        return Arbitraries.of(true, false);
    }
    
    // Helper methods
    
    private User createMockUser(String email, String fullName) {
        User user = new User(email, fullName);
        user.setEmailVerified(true);
        return user;
    }
}