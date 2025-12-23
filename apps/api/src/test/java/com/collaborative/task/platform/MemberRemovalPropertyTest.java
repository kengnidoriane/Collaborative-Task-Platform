package com.collaborative.task.platform;

import com.collaborative.task.platform.entity.Project;
import com.collaborative.task.platform.entity.ProjectMember;
import com.collaborative.task.platform.entity.ProjectRole;
import com.collaborative.task.platform.entity.User;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for team member removal functionality.
 * **Feature: collaborative-task-platform, Property 9: Team member removal revokes access**
 * **Validates: Requirements 2.4**
 */
public class MemberRemovalPropertyTest {
    
    /**
     * Property 9: Team member removal revokes access
     * For any team member removal by a project owner, the system should revoke all access and update project visibility appropriately.
     */
    @Property(tries = 100)
    void memberRemovalRevokesAccess(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validRoles") ProjectRole memberRole) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Project Owner");
        User member = createMockUser("member@example.com", "Team Member");
        Project project = new Project(projectName, "Description", owner, false);
        
        // Add member to project
        project.addMember(member, memberRole);
        
        // Verify member has access before removal
        assertThat(project.isMember(member)).isTrue();
        assertThat(project.getMemberRole(member)).isEqualTo(memberRole);
        int initialMemberCount = project.getMemberCount();
        
        // Act - Remove member from project
        project.removeMember(member);
        
        // Assert - Member access is revoked
        assertThat(project.isMember(member)).isFalse();
        assertThat(project.getMemberRole(member)).isNull();
        assertThat(project.getMemberCount()).isEqualTo(initialMemberCount - 1);
        
        // Assert - Owner still has access (cannot be removed through member management)
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.isMember(owner)).isTrue();
        assertThat(project.hasAdminAccess(owner)).isTrue();
    }
    
    /**
     * Property: Multiple member removal works correctly
     * For any project with multiple members, removing members should work independently.
     */
    @Property(tries = 50)
    void multipleMemberRemovalWorksCorrectly(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validRoles") ProjectRole role1,
            @ForAll("validRoles") ProjectRole role2) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member1 = createMockUser("member1@example.com", "Member One");
        User member2 = createMockUser("member2@example.com", "Member Two");
        Project project = new Project(projectName, "Description", owner, false);
        
        // Add both members
        project.addMember(member1, role1);
        project.addMember(member2, role2);
        
        // Verify both members have access
        assertThat(project.isMember(member1)).isTrue();
        assertThat(project.isMember(member2)).isTrue();
        assertThat(project.getMemberCount()).isEqualTo(3); // Owner + 2 members
        
        // Act - Remove first member
        project.removeMember(member1);
        
        // Assert - Only first member is removed
        assertThat(project.isMember(member1)).isFalse();
        assertThat(project.isMember(member2)).isTrue();
        assertThat(project.getMemberCount()).isEqualTo(2); // Owner + 1 member
        
        // Act - Remove second member
        project.removeMember(member2);
        
        // Assert - Both members are removed, only owner remains
        assertThat(project.isMember(member1)).isFalse();
        assertThat(project.isMember(member2)).isFalse();
        assertThat(project.isMember(owner)).isTrue();
        assertThat(project.getMemberCount()).isEqualTo(1); // Only owner
    }
    
    /**
     * Property: Removing non-existent member is safe
     * For any project, attempting to remove a user who is not a member should be safe.
     */
    @Property(tries = 30)
    void removingNonExistentMemberIsSafe(
            @ForAll("validProjectNames") String projectName) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User nonMember = createMockUser("nonmember@example.com", "Non Member");
        Project project = new Project(projectName, "Description", owner, false);
        
        int initialMemberCount = project.getMemberCount();
        
        // Verify user is not a member
        assertThat(project.isMember(nonMember)).isFalse();
        
        // Act - Attempt to remove non-member (should be safe)
        project.removeMember(nonMember);
        
        // Assert - Project state is unchanged
        assertThat(project.isMember(nonMember)).isFalse();
        assertThat(project.getMemberCount()).isEqualTo(initialMemberCount);
        assertThat(project.isMember(owner)).isTrue();
    }
    
    /**
     * Property: Member removal preserves other members' access
     * For any project with multiple members, removing one member should not affect others.
     */
    @Property(tries = 30)
    void memberRemovalPreservesOtherAccess(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validRoles") ProjectRole keepRole,
            @ForAll("validRoles") ProjectRole removeRole) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User keepMember = createMockUser("keep@example.com", "Keep Member");
        User removeMember = createMockUser("remove@example.com", "Remove Member");
        Project project = new Project(projectName, "Description", owner, false);
        
        // Add both members
        project.addMember(keepMember, keepRole);
        project.addMember(removeMember, removeRole);
        
        // Verify both members have access
        assertThat(project.isMember(keepMember)).isTrue();
        assertThat(project.isMember(removeMember)).isTrue();
        assertThat(project.getMemberRole(keepMember)).isEqualTo(keepRole);
        assertThat(project.getMemberRole(removeMember)).isEqualTo(removeRole);
        
        // Act - Remove one member
        project.removeMember(removeMember);
        
        // Assert - Kept member still has same access
        assertThat(project.isMember(keepMember)).isTrue();
        assertThat(project.getMemberRole(keepMember)).isEqualTo(keepRole);
        assertThat(project.hasAdminAccess(keepMember)).isEqualTo(keepRole.isAdmin());
        
        // Assert - Removed member has no access
        assertThat(project.isMember(removeMember)).isFalse();
        assertThat(project.getMemberRole(removeMember)).isNull();
        
        // Assert - Owner still has access
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.hasAdminAccess(owner)).isTrue();
    }
    
    /**
     * Property: Member removal and re-addition works correctly
     * For any member, removing and then re-adding should work correctly.
     */
    @Property(tries = 20)
    void memberRemovalAndReAdditionWorks(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validRoles") ProjectRole originalRole,
            @ForAll("validRoles") ProjectRole newRole) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member = createMockUser("member@example.com", "Member");
        Project project = new Project(projectName, "Description", owner, false);
        
        // Add member with original role
        project.addMember(member, originalRole);
        assertThat(project.isMember(member)).isTrue();
        assertThat(project.getMemberRole(member)).isEqualTo(originalRole);
        
        // Act - Remove member
        project.removeMember(member);
        assertThat(project.isMember(member)).isFalse();
        
        // Act - Re-add member with new role
        project.addMember(member, newRole);
        
        // Assert - Member has new role and access
        assertThat(project.isMember(member)).isTrue();
        assertThat(project.getMemberRole(member)).isEqualTo(newRole);
        assertThat(project.hasAdminAccess(member)).isEqualTo(newRole.isAdmin());
    }
    
    /**
     * Test that validates basic member removal functionality.
     */
    @Test
    void basicMemberRemovalWorks() {
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member = createMockUser("member@example.com", "Member");
        Project project = new Project("Test Project", "Description", owner, false);
        
        project.addMember(member, ProjectRole.MEMBER);
        assertThat(project.isMember(member)).isTrue();
        assertThat(project.getMemberCount()).isEqualTo(2);
        
        // Act
        project.removeMember(member);
        
        // Assert
        assertThat(project.isMember(member)).isFalse();
        assertThat(project.getMemberCount()).isEqualTo(1);
        assertThat(project.isMember(owner)).isTrue();
    }
    
    /**
     * Test that validates owner cannot be removed through member management.
     */
    @Test
    void ownerCannotBeRemovedThroughMemberManagement() {
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        Project project = new Project("Test Project", "Description", owner, false);
        
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.isMember(owner)).isTrue();
        int initialCount = project.getMemberCount();
        
        // Act - Attempt to remove owner through member management
        project.removeMember(owner);
        
        // Assert - Owner is still there (removal should be safe but ineffective)
        assertThat(project.isOwner(owner)).isTrue();
        assertThat(project.isMember(owner)).isTrue();
        assertThat(project.getMemberCount()).isEqualTo(initialCount);
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