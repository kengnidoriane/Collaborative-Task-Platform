package com.collaborative.task.platform.repository;

import com.collaborative.task.platform.entity.Project;
import com.collaborative.task.platform.entity.ProjectMember;
import com.collaborative.task.platform.entity.ProjectMemberId;
import com.collaborative.task.platform.entity.ProjectRole;
import com.collaborative.task.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProjectMember entity operations.
 * Provides clean data access layer for project membership management.
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    
    /**
     * Find all members of a specific project.
     * Used for team management and member listing.
     */
    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.user WHERE pm.project = :project ORDER BY pm.joinedAt ASC")
    List<ProjectMember> findByProjectOrderByJoinedAtAsc(Project project);
    
    /**
     * Find all projects where user is a member.
     * Used for user's project membership tracking.
     */
    List<ProjectMember> findByUserOrderByJoinedAtDesc(User user);
    
    /**
     * Find specific project membership.
     * Used for role checking and membership validation.
     */
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
    
    /**
     * Find pending invitations for a user.
     * Used for invitation management and notifications.
     */
    @Query("""
        SELECT pm FROM ProjectMember pm 
        JOIN FETCH pm.project 
        WHERE pm.user = :user 
        AND pm.invitationAcceptedAt IS NULL 
        ORDER BY pm.joinedAt DESC
        """)
    List<ProjectMember> findPendingInvitationsByUser(@Param("user") User user);
    
    /**
     * Find pending invitations for a project.
     * Used for project invitation management.
     */
    @Query("""
        SELECT pm FROM ProjectMember pm 
        JOIN FETCH pm.user 
        WHERE pm.project = :project 
        AND pm.invitationAcceptedAt IS NULL 
        ORDER BY pm.joinedAt DESC
        """)
    List<ProjectMember> findPendingInvitationsByProject(@Param("project") Project project);
    
    /**
     * Find accepted members of a project with specific role.
     * Used for role-based access control and team management.
     */
    @Query("""
        SELECT pm FROM ProjectMember pm 
        JOIN FETCH pm.user 
        WHERE pm.project = :project 
        AND pm.role = :role 
        AND pm.invitationAcceptedAt IS NOT NULL 
        ORDER BY pm.joinedAt ASC
        """)
    List<ProjectMember> findAcceptedMembersByProjectAndRole(@Param("project") Project project, @Param("role") ProjectRole role);
    
    /**
     * Count accepted members of a project.
     * Used for team size analytics and limits.
     */
    @Query("""
        SELECT COUNT(pm) FROM ProjectMember pm 
        WHERE pm.project = :project 
        AND pm.invitationAcceptedAt IS NOT NULL
        """)
    long countAcceptedMembersByProject(@Param("project") Project project);
    
    /**
     * Find members who joined within a date range.
     * Used for analytics and activity tracking.
     */
    @Query("""
        SELECT pm FROM ProjectMember pm 
        JOIN FETCH pm.user 
        WHERE pm.project = :project 
        AND pm.invitationAcceptedAt BETWEEN :startDate AND :endDate 
        ORDER BY pm.invitationAcceptedAt DESC
        """)
    List<ProjectMember> findMembersJoinedBetween(@Param("project") Project project, 
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Check if user is a member of project with accepted invitation.
     * Used for quick access validation.
     */
    @Query("""
        SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END 
        FROM ProjectMember pm 
        WHERE pm.project.id = :projectId 
        AND pm.user = :user 
        AND pm.invitationAcceptedAt IS NOT NULL
        """)
    boolean isAcceptedMember(@Param("projectId") UUID projectId, @Param("user") User user);
    
    /**
     * Find all admin members (ADMIN or OWNER role) of a project.
     * Used for administrative operations and notifications.
     */
    @Query("""
        SELECT pm FROM ProjectMember pm 
        JOIN FETCH pm.user 
        WHERE pm.project = :project 
        AND pm.role IN ('ADMIN', 'OWNER') 
        AND pm.invitationAcceptedAt IS NOT NULL 
        ORDER BY pm.role ASC, pm.joinedAt ASC
        """)
    List<ProjectMember> findAdminMembersByProject(@Param("project") Project project);
    
    /**
     * Delete all members of a project (used for project deletion).
     * Used for cleanup operations.
     */
    void deleteByProject(Project project);
    
    /**
     * Find members invited by a specific user.
     * Used for invitation tracking and audit.
     */
    @Query("""
        SELECT pm FROM ProjectMember pm 
        JOIN FETCH pm.user 
        WHERE pm.project = :project 
        AND pm.invitedBy = :inviterId 
        ORDER BY pm.joinedAt DESC
        """)
    List<ProjectMember> findMembersInvitedBy(@Param("project") Project project, @Param("inviterId") UUID inviterId);
}