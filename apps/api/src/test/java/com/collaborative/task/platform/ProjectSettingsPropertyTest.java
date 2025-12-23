package com.collaborative.task.platform;

import com.collaborative.task.platform.entity.Project;
import com.collaborative.task.platform.entity.ProjectRole;
import com.collaborative.task.platform.entity.User;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for project settings functionality.
 * **Feature: collaborative-task-platform, Property 8: Project settings updates propagate correctly**
 * **Validates: Requirements 2.3**
 */
public class ProjectSettingsPropertyTest {
    
    /**
     * Property 8: Project settings updates propagate correctly
     * For any project setting modification by an owner, the system should update configurations and notify affected team members.
     */
    @Property(tries = 100)
    void projectSettingsUpdatesPropagateCorrectly(
            @ForAll("validProjectNames") String originalName,
            @ForAll("validProjectNames") String newName,
            @ForAll("validDescriptions") String originalDescription,
            @ForAll("validDescriptions") String newDescription,
            @ForAll("validPrivacySettings") boolean originalPrivacy,
            @ForAll("validPrivacySettings") boolean newPrivacy) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Project Owner");
        User member = createMockUser("member@example.com", "Team Member");
        Project project = new Project(originalName, originalDescription, owner, originalPrivacy);
        
        // Add a team member to verify settings affect all members
        project.addMember(member, ProjectRole.MEMBER);
        
        // Verify initial state
        assertThat(project.getName()).isEqualTo(originalName);
        assertThat(project.getDescription()).isEqualTo(originalDescription);
        assertThat(project.isPrivate()).isEqualTo(originalPrivacy);
        assertThat(project.isArchived()).isFalse();
        
        // Act - Update project settings
        project.setName(newName);
        project.setDescription(newDescription);
        project.setPrivate(newPrivacy);
        
        // Assert - Settings are updated correctly
        assertThat(project.getName()).isEqualTo(newName);
        assertThat(project.getDescription()).isEqualTo(newDescription);
        assertThat(project.isPrivate()).isEqualTo(newPrivacy);
        
        // Assert - Project structure remains intact
        assertThat(project.getOwner()).isEqualTo(owner);
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.isMember(member)).isTrue();
        assertThat(project.getMemberCount()).isEqualTo(2);
        assertThat(project.isArchived()).isFalse();
        
        // Assert - Member access is preserved
        assertThat(project.getMemberRole(member)).isEqualTo(ProjectRole.MEMBER);
        assertThat(project.hasAdminAccess(owner)).isTrue();
        assertThat(project.hasAdminAccess(member)).isFalse();
    }
    
    /**
     * Property: Project archival settings work correctly
     * For any project, archival should preserve all data while changing accessibility.
     */
    @Property(tries = 50)
    void projectArchivalSettingsWorkCorrectly(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validDescriptions") String description,
            @ForAll("validPrivacySettings") boolean isPrivate) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member = createMockUser("member@example.com", "Member");
        Project project = new Project(projectName, description, owner, isPrivate);
        project.addMember(member, ProjectRole.MEMBER);
        
        // Verify initial active state
        assertThat(project.isArchived()).isFalse();
        assertThat(project.isActive()).isTrue();
        
        // Act - Archive project
        project.setArchived(true);
        
        // Assert - Project is archived but data is preserved
        assertThat(project.isArchived()).isTrue();
        assertThat(project.isActive()).isFalse();
        assertThat(project.getName()).isEqualTo(projectName);
        assertThat(project.getDescription()).isEqualTo(description);
        assertThat(project.isPrivate()).isEqualTo(isPrivate);
        assertThat(project.getOwner()).isEqualTo(owner);
        assertThat(project.getMemberCount()).isEqualTo(2);
        
        // Act - Unarchive project
        project.setArchived(false);
        
        // Assert - Project is restored to active state
        assertThat(project.isArchived()).isFalse();
        assertThat(project.isActive()).isTrue();
        assertThat(project.getName()).isEqualTo(projectName);
        assertThat(project.isMember(member)).isTrue();
    }
    
    /**
     * Property: Privacy settings affect project visibility correctly
     * For any project, changing privacy settings should work consistently.
     */
    @Property(tries = 30)
    void privacySettingsAffectVisibilityCorrectly(
            @ForAll("validProjectNames") String projectName) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member = createMockUser("member@example.com", "Member");
        
        // Test public project
        Project publicProject = new Project(projectName, "Description", owner, false);
        publicProject.addMember(member, ProjectRole.MEMBER);
        
        assertThat(publicProject.isPrivate()).isFalse();
        
        // Act - Make project private
        publicProject.setPrivate(true);
        
        // Assert - Privacy is updated, members preserved
        assertThat(publicProject.isPrivate()).isTrue();
        assertThat(publicProject.isMember(member)).isTrue();
        assertThat(publicProject.getMemberCount()).isEqualTo(2);
        
        // Act - Make project public again
        publicProject.setPrivate(false);
        
        // Assert - Privacy is updated, members preserved
        assertThat(publicProject.isPrivate()).isFalse();
        assertThat(publicProject.isMember(member)).isTrue();
    }
    
    /**
     * Property: Multiple settings updates work correctly
     * For any project, updating multiple settings simultaneously should work correctly.
     */
    @Property(tries = 30)
    void multipleSettingsUpdatesWorkCorrectly(
            @ForAll("validProjectNames") String originalName,
            @ForAll("validProjectNames") String newName,
            @ForAll("validDescriptions") String newDescription) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        Project project = new Project(originalName, "Original Description", owner, false);
        
        // Act - Update multiple settings at once
        project.setName(newName);
        project.setDescription(newDescription);
        project.setPrivate(true);
        project.setArchived(true);
        
        // Assert - All settings are updated correctly
        assertThat(project.getName()).isEqualTo(newName);
        assertThat(project.getDescription()).isEqualTo(newDescription);
        assertThat(project.isPrivate()).isTrue();
        assertThat(project.isArchived()).isTrue();
        assertThat(project.isActive()).isFalse();
        
        // Assert - Core project identity is preserved
        assertThat(project.getOwner()).isEqualTo(owner);
        assertThat(project.isOwner(owner)).isTrue();
    }
    
    /**
     * Property: Settings updates preserve member relationships
     * For any project with members, settings updates should not affect member relationships.
     */
    @Property(tries = 20)
    void settingsUpdatesPreserveMemberRelationships(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validRoles") ProjectRole memberRole) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User admin = createMockUser("admin@example.com", "Admin");
        User member = createMockUser("member@example.com", "Member");
        Project project = new Project(projectName, "Description", owner, false);
        
        project.addMember(admin, ProjectRole.ADMIN);
        project.addMember(member, memberRole);
        
        // Verify initial member state
        assertThat(project.getMemberCount()).isEqualTo(3);
        assertThat(project.hasAdminAccess(admin)).isTrue();
        assertThat(project.getMemberRole(member)).isEqualTo(memberRole);
        
        // Act - Update various settings
        project.setName("Updated " + projectName);
        project.setDescription("Updated Description");
        project.setPrivate(true);
        
        // Assert - Member relationships are preserved
        assertThat(project.getMemberCount()).isEqualTo(3);
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.hasAdminAccess(admin)).isTrue();
        assertThat(project.getMemberRole(member)).isEqualTo(memberRole);
        assertThat(project.isMember(admin)).isTrue();
        assertThat(project.isMember(member)).isTrue();
    }
    
    /**
     * Test that validates basic project settings functionality.
     */
    @Test
    void basicProjectSettingsWork() {
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        Project project = new Project("Original Name", "Original Description", owner, false);
        
        // Act
        project.setName("New Name");
        project.setDescription("New Description");
        project.setPrivate(true);
        
        // Assert
        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("New Description");
        assertThat(project.isPrivate()).isTrue();
        assertThat(project.getOwner()).isEqualTo(owner);
    }
    
    /**
     * Test that validates project archival functionality.
     */
    @Test
    void projectArchivalWorks() {
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        Project project = new Project("Test Project", "Description", owner, false);
        
        assertThat(project.isArchived()).isFalse();
        assertThat(project.isActive()).isTrue();
        
        // Act
        project.setArchived(true);
        
        // Assert
        assertThat(project.isArchived()).isTrue();
        assertThat(project.isActive()).isFalse();
        
        // Act - Restore
        project.setArchived(false);
        
        // Assert
        assertThat(project.isArchived()).isFalse();
        assertThat(project.isActive()).isTrue();
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
    
    @Provide
    Arbitrary<ProjectRole> validRoles() {
        // Don't include OWNER role as it should be handled separately
        return Arbitraries.of(ProjectRole.ADMIN, ProjectRole.MEMBER, ProjectRole.VIEWER);
    }
    
    // Helper methods
    
    private User createMockUser(String email, String fullName) {
        User user = new User(email, fullName);
        user.setEmailVerified(true);
        return user;
    }
}