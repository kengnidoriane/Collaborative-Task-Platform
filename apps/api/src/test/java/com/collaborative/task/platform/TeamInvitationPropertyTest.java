package com.collaborative.task.platform;

import com.collaborative.task.platform.entity.Project;
import com.collaborative.task.platform.entity.ProjectMember;
import com.collaborative.task.platform.entity.ProjectRole;
import com.collaborative.task.platform.entity.User;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for team invitation functionality.
 * **Feature: collaborative-task-platform, Property 7: Team invitation workflow is complete**
 * **Validates: Requirements 2.2**
 */
public class TeamInvitationPropertyTest {
    
    /**
     * Property 7: Team invitation workflow is complete
     * For any valid team member invitation, the system should send invitations and grant appropriate access upon acceptance.
     */
    @Property(tries = 100)
    void teamInvitationWorkflowIsComplete(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validEmails") String inviteeEmail,
            @ForAll("validRoles") ProjectRole role) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Project Owner");
        User invitee = createMockUser(inviteeEmail, "Team Member");
        Project project = new Project(projectName, "Description", owner, false);
        
        // Act - Create invitation (simulating service layer logic)
        ProjectMember invitation = new ProjectMember(project, invitee, role, owner.getId());
        
        // Assert - Invitation is created correctly
        assertThat(invitation).isNotNull();
        assertThat(invitation.getProject()).isEqualTo(project);
        assertThat(invitation.getUser()).isEqualTo(invitee);
        assertThat(invitation.getRole()).isEqualTo(role);
        assertThat(invitation.getInvitedBy()).isEqualTo(owner.getId());
        assertThat(invitation.isPendingInvitation()).isTrue();
        assertThat(invitation.getInvitationAcceptedAt()).isNull();
        
        // Act - Accept invitation
        invitation.acceptInvitation();
        
        // Assert - Invitation is accepted and access is granted
        assertThat(invitation.isPendingInvitation()).isFalse();
        assertThat(invitation.getInvitationAcceptedAt()).isNotNull();
        assertThat(invitation.getInvitationAcceptedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        
        // Assert - User has appropriate access based on role
        assertThat(invitation.hasAdminAccess()).isEqualTo(role.isAdmin());
        assertThat(invitation.canModify()).isEqualTo(role.canModify());
        assertThat(invitation.canManageMembers()).isEqualTo(role.canManageMembers());
    }
    
    /**
     * Property: Invitation role permissions are consistent
     * For any role assigned to an invitation, the permissions should match the role definition.
     */
    @Property(tries = 50)
    void invitationRolePermissionsAreConsistent(@ForAll("validRoles") ProjectRole role) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User invitee = createMockUser("member@example.com", "Member");
        Project project = new Project("Test Project", "Description", owner, false);
        
        // Act
        ProjectMember invitation = new ProjectMember(project, invitee, role, owner.getId());
        invitation.acceptInvitation();
        
        // Assert - Role permissions are consistent
        assertThat(invitation.getRole()).isEqualTo(role);
        assertThat(invitation.hasAdminAccess()).isEqualTo(role.isAdmin());
        assertThat(invitation.canModify()).isEqualTo(role.canModify());
        assertThat(invitation.canManageMembers()).isEqualTo(role.canManageMembers());
        
        // Assert - Role hierarchy is respected
        if (role == ProjectRole.OWNER) {
            assertThat(invitation.hasAdminAccess()).isTrue();
            assertThat(invitation.canModify()).isTrue();
            assertThat(invitation.canManageMembers()).isTrue();
        } else if (role == ProjectRole.ADMIN) {
            assertThat(invitation.hasAdminAccess()).isTrue();
            assertThat(invitation.canModify()).isTrue();
            assertThat(invitation.canManageMembers()).isTrue();
        } else if (role == ProjectRole.MEMBER) {
            assertThat(invitation.hasAdminAccess()).isFalse();
            assertThat(invitation.canModify()).isTrue();
            assertThat(invitation.canManageMembers()).isFalse();
        } else if (role == ProjectRole.VIEWER) {
            assertThat(invitation.hasAdminAccess()).isFalse();
            assertThat(invitation.canModify()).isFalse();
            assertThat(invitation.canManageMembers()).isFalse();
        }
    }
    
    /**
     * Property: Multiple invitations to same project work correctly
     * For any project, multiple users can be invited with different roles.
     */
    @Property(tries = 30)
    void multipleInvitationsWorkCorrectly(
            @ForAll("validProjectNames") String projectName,
            @ForAll("validRoles") ProjectRole role1,
            @ForAll("validRoles") ProjectRole role2) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member1 = createMockUser("member1@example.com", "Member One");
        User member2 = createMockUser("member2@example.com", "Member Two");
        Project project = new Project(projectName, "Description", owner, false);
        
        // Act - Create multiple invitations
        ProjectMember invitation1 = new ProjectMember(project, member1, role1, owner.getId());
        ProjectMember invitation2 = new ProjectMember(project, member2, role2, owner.getId());
        
        // Assert - Both invitations are independent
        assertThat(invitation1.getUser()).isNotEqualTo(invitation2.getUser());
        assertThat(invitation1.getRole()).isEqualTo(role1);
        assertThat(invitation2.getRole()).isEqualTo(role2);
        assertThat(invitation1.getProject()).isEqualTo(invitation2.getProject());
        
        // Act - Accept one invitation
        invitation1.acceptInvitation();
        
        // Assert - Only accepted invitation is active
        assertThat(invitation1.isPendingInvitation()).isFalse();
        assertThat(invitation2.isPendingInvitation()).isTrue();
        assertThat(invitation1.getInvitationAcceptedAt()).isNotNull();
        assertThat(invitation2.getInvitationAcceptedAt()).isNull();
        
        // Act - Accept second invitation
        invitation2.acceptInvitation();
        
        // Assert - Both invitations are now active
        assertThat(invitation1.isPendingInvitation()).isFalse();
        assertThat(invitation2.isPendingInvitation()).isFalse();
        assertThat(invitation1.getInvitationAcceptedAt()).isNotNull();
        assertThat(invitation2.getInvitationAcceptedAt()).isNotNull();
    }
    
    /**
     * Property: Invitation timing is consistent
     * For any invitation, the timing fields should be logically consistent.
     */
    @Property(tries = 20)
    void invitationTimingIsConsistent(@ForAll("validRoles") ProjectRole role) {
        
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User invitee = createMockUser("invitee@example.com", "Invitee");
        Project project = new Project("Test Project", "Description", owner, false);
        
        LocalDateTime beforeInvitation = LocalDateTime.now();
        
        // Act - Create invitation
        ProjectMember invitation = new ProjectMember(project, invitee, role, owner.getId());
        
        LocalDateTime afterInvitation = LocalDateTime.now();
        
        // Assert - Invitation timing is correct
        assertThat(invitation.getJoinedAt()).isNotNull();
        assertThat(invitation.getJoinedAt()).isAfter(beforeInvitation.minusSeconds(1));
        assertThat(invitation.getJoinedAt()).isBefore(afterInvitation.plusSeconds(1));
        assertThat(invitation.isPendingInvitation()).isTrue();
        
        LocalDateTime beforeAcceptance = LocalDateTime.now();
        
        // Act - Accept invitation
        invitation.acceptInvitation();
        
        LocalDateTime afterAcceptance = LocalDateTime.now();
        
        // Assert - Acceptance timing is correct
        assertThat(invitation.getInvitationAcceptedAt()).isNotNull();
        assertThat(invitation.getInvitationAcceptedAt()).isAfter(beforeAcceptance.minusSeconds(1));
        assertThat(invitation.getInvitationAcceptedAt()).isBefore(afterAcceptance.plusSeconds(1));
        assertThat(invitation.getInvitationAcceptedAt()).isAfterOrEqualTo(invitation.getJoinedAt());
    }
    
    /**
     * Test that validates basic invitation functionality.
     */
    @Test
    void basicInvitationWorks() {
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member = createMockUser("member@example.com", "Member");
        Project project = new Project("Test Project", "Description", owner, false);
        
        // Act
        ProjectMember invitation = new ProjectMember(project, member, ProjectRole.MEMBER, owner.getId());
        
        // Assert
        assertThat(invitation).isNotNull();
        assertThat(invitation.isPendingInvitation()).isTrue();
        assertThat(invitation.getRole()).isEqualTo(ProjectRole.MEMBER);
        
        // Act - Accept
        invitation.acceptInvitation();
        
        // Assert
        assertThat(invitation.isPendingInvitation()).isFalse();
        assertThat(invitation.getInvitationAcceptedAt()).isNotNull();
    }
    
    /**
     * Test that validates invitation equality and identity.
     */
    @Test
    void invitationEqualityWorks() {
        // Arrange
        User owner = createMockUser("owner@example.com", "Owner");
        User member = createMockUser("member@example.com", "Member");
        Project project = new Project("Test Project", "Description", owner, false);
        
        // Act
        ProjectMember invitation1 = new ProjectMember(project, member, ProjectRole.MEMBER, owner.getId());
        ProjectMember invitation2 = new ProjectMember(project, member, ProjectRole.ADMIN, owner.getId());
        
        // Assert - Same project and user should be equal regardless of role
        assertThat(invitation1).isEqualTo(invitation2);
        assertThat(invitation1.hashCode()).isEqualTo(invitation2.hashCode());
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
    Arbitrary<String> validEmails() {
        return Combinators.combine(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(10),
                Arbitraries.of("gmail.com", "yahoo.com", "outlook.com", "company.com")
        ).as((username, domain) -> username + "@" + domain);
    }
    
    @Provide
    Arbitrary<ProjectRole> validRoles() {
        // Don't include OWNER role in invitations as it should be handled separately
        return Arbitraries.of(ProjectRole.ADMIN, ProjectRole.MEMBER, ProjectRole.VIEWER);
    }
    
    // Helper methods
    
    private User createMockUser(String email, String fullName) {
        User user = new User(email, fullName);
        user.setEmailVerified(true);
        return user;
    }
}