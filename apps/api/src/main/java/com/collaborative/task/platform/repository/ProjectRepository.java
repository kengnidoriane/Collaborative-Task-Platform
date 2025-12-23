package com.collaborative.task.platform.repository;

import com.collaborative.task.platform.entity.Project;
import com.collaborative.task.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Project entity operations.
 * Provides clean data access layer with optimized queries for project management.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    /**
     * Find all projects owned by a specific user.
     * Used for user's project dashboard.
     */
    List<Project> findByOwnerAndArchivedFalseOrderByUpdatedAtDesc(User owner);
    
    /**
     * Find all active projects where user is a member (including owned projects).
     * Used for user's accessible projects list.
     */
    @Query("""
        SELECT DISTINCT p FROM Project p 
        LEFT JOIN p.members m 
        WHERE p.archived = false 
        AND (p.owner = :user OR (m.user = :user AND m.invitationAcceptedAt IS NOT NULL))
        ORDER BY p.updatedAt DESC
        """)
    List<Project> findAccessibleProjectsByUser(@Param("user") User user);
    
    /**
     * Find projects by name containing search term (case-insensitive).
     * Used for project search functionality.
     */
    @Query("""
        SELECT DISTINCT p FROM Project p 
        LEFT JOIN p.members m 
        WHERE p.archived = false 
        AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        AND (p.isPrivate = false OR p.owner = :user OR (m.user = :user AND m.invitationAcceptedAt IS NOT NULL))
        ORDER BY p.updatedAt DESC
        """)
    List<Project> findProjectsByNameContaining(@Param("searchTerm") String searchTerm, @Param("user") User user);
    
    /**
     * Find public projects (not private) for discovery.
     * Used for public project browsing.
     */
    Page<Project> findByIsPrivateFalseAndArchivedFalseOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * Find projects created within a date range.
     * Used for analytics and reporting.
     */
    @Query("SELECT p FROM Project p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Project> findProjectsCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find projects with member count greater than threshold.
     * Used for analytics and team size analysis.
     */
    @Query("""
        SELECT p FROM Project p 
        WHERE p.archived = false 
        AND SIZE(p.members) > :memberCount
        ORDER BY SIZE(p.members) DESC
        """)
    List<Project> findProjectsWithMemberCountGreaterThan(@Param("memberCount") int memberCount);
    
    /**
     * Find recently updated projects for a user.
     * Used for activity feeds and notifications.
     */
    @Query("""
        SELECT DISTINCT p FROM Project p 
        LEFT JOIN p.members m 
        WHERE p.archived = false 
        AND p.updatedAt > :since
        AND (p.owner = :user OR (m.user = :user AND m.invitationAcceptedAt IS NOT NULL))
        ORDER BY p.updatedAt DESC
        """)
    List<Project> findRecentlyUpdatedProjectsForUser(@Param("user") User user, @Param("since") LocalDateTime since);
    
    /**
     * Check if user has access to a specific project.
     * Used for authorization checks.
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END 
        FROM Project p 
        LEFT JOIN p.members m 
        WHERE p.id = :projectId 
        AND p.archived = false
        AND (p.owner = :user OR (m.user = :user AND m.invitationAcceptedAt IS NOT NULL))
        """)
    boolean hasUserAccessToProject(@Param("projectId") UUID projectId, @Param("user") User user);
    
    /**
     * Find project with members eagerly loaded.
     * Used when full project details with team are needed.
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.members m LEFT JOIN FETCH m.user WHERE p.id = :projectId")
    Optional<Project> findByIdWithMembers(@Param("projectId") UUID projectId);
    
    /**
     * Count total projects owned by user.
     * Used for user statistics and limits.
     */
    long countByOwnerAndArchivedFalse(User owner);
    
    /**
     * Find archived projects for a user (for restoration purposes).
     * Used for project archive management.
     */
    @Query("""
        SELECT DISTINCT p FROM Project p 
        LEFT JOIN p.members m 
        WHERE p.archived = true 
        AND (p.owner = :user OR (m.user = :user AND m.invitationAcceptedAt IS NOT NULL))
        ORDER BY p.updatedAt DESC
        """)
    List<Project> findArchivedProjectsForUser(@Param("user") User user);
}