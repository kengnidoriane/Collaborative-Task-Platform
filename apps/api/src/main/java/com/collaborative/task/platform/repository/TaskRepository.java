package com.collaborative.task.platform.repository;

import com.collaborative.task.platform.entity.Task;
import com.collaborative.task.platform.entity.TaskStatus;
import com.collaborative.task.platform.entity.TaskPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Task entity with optimized queries.
 * 
 * This repository follows clean architecture principles with:
 * - Optimized queries using JPQL and native SQL where appropriate
 * - Proper indexing strategy for performance
 * - Clear method naming following Spring Data conventions
 * - Batch operations for efficiency
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    // Basic queries with optimized fetching
    
    /**
     * Finds all tasks in a project with eager loading of necessary associations
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.createdBy " +
           "WHERE t.project.id = :projectId " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findByProjectIdWithAssignees(@Param("projectId") UUID projectId);
    
    /**
     * Finds tasks by project with pagination and sorting
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.createdBy " +
           "WHERE t.project.id = :projectId")
    Page<Task> findByProjectId(@Param("projectId") UUID projectId, Pageable pageable);
    
    /**
     * Finds tasks assigned to a specific user
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.project " +
           "LEFT JOIN FETCH t.createdBy " +
           "WHERE t.assignee.id = :userId " +
           "ORDER BY t.priority DESC, t.dueDate ASC NULLS LAST")
    List<Task> findByAssigneeIdOrderByPriorityAndDueDate(@Param("userId") UUID userId);
    
    /**
     * Finds tasks by status within a project
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.project.id = :projectId AND t.status = :status " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findByProjectIdAndStatus(@Param("projectId") UUID projectId, 
                                       @Param("status") TaskStatus status);
    
    /**
     * Finds overdue tasks across all projects
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.project " +
           "WHERE t.dueDate < :currentTime AND t.status NOT IN :completedStatuses " +
           "ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasks(@Param("currentTime") LocalDateTime currentTime,
                               @Param("completedStatuses") List<TaskStatus> completedStatuses);
    
    /**
     * Finds tasks by priority within a project
     */
    List<Task> findByProjectIdAndPriorityOrderByCreatedAtAsc(UUID projectId, TaskPriority priority);
    
    /**
     * Finds tasks created by a specific user
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.project " +
           "WHERE t.createdBy.id = :userId " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByCreatedByIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    // Advanced queries for analytics and reporting
    
    /**
     * Counts tasks by status for a project
     */
    @Query("SELECT t.status, COUNT(t) FROM Task t " +
           "WHERE t.project.id = :projectId " +
           "GROUP BY t.status")
    List<Object[]> countTasksByStatusForProject(@Param("projectId") UUID projectId);
    
    /**
     * Counts tasks by priority for a project
     */
    @Query("SELECT t.priority, COUNT(t) FROM Task t " +
           "WHERE t.project.id = :projectId " +
           "GROUP BY t.priority")
    List<Object[]> countTasksByPriorityForProject(@Param("projectId") UUID projectId);
    
    /**
     * Gets task completion statistics for a project
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN t.status = 'DONE' THEN 1 END) as completed, " +
           "COUNT(t) as total, " +
           "AVG(CASE WHEN t.actualHours IS NOT NULL THEN t.actualHours ELSE 0 END) as avgHours " +
           "FROM Task t WHERE t.project.id = :projectId")
    Object[] getTaskStatisticsForProject(@Param("projectId") UUID projectId);
    
    /**
     * Finds tasks with dependencies
     */
    @Query("SELECT t FROM Task t WHERE SIZE(t.dependsOn) > 0 AND t.project.id = :projectId")
    List<Task> findTasksWithDependencies(@Param("projectId") UUID projectId);
    
    /**
     * Finds tasks that depend on a specific task
     */
    @Query("SELECT t FROM Task t WHERE :taskId MEMBER OF t.dependsOn")
    List<Task> findTasksDependingOn(@Param("taskId") UUID taskId);
    
    // Search and filtering queries
    
    /**
     * Searches tasks by title or description
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.project.id = :projectId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> searchTasksInProject(@Param("projectId") UUID projectId, 
                                   @Param("searchTerm") String searchTerm);
    
    /**
     * Finds tasks by tag
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.project.id = :projectId AND :tag MEMBER OF t.tags " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findByProjectIdAndTag(@Param("projectId") UUID projectId, 
                                    @Param("tag") String tag);
    
    /**
     * Finds tasks due within a specific time range
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignee " +
           "WHERE t.project.id = :projectId AND " +
           "t.dueDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.dueDate ASC")
    List<Task> findTasksDueBetween(@Param("projectId") UUID projectId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    // Batch operations
    
    /**
     * Updates task status in batch
     */
    @Modifying
    @Query("UPDATE Task t SET t.status = :newStatus, t.updatedAt = :updateTime " +
           "WHERE t.id IN :taskIds")
    int updateTaskStatusBatch(@Param("taskIds") List<UUID> taskIds,
                             @Param("newStatus") TaskStatus newStatus,
                             @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * Updates task assignee in batch
     */
    @Modifying
    @Query("UPDATE Task t SET t.assignee.id = :assigneeId, t.updatedAt = :updateTime " +
           "WHERE t.id IN :taskIds")
    int updateTaskAssigneeBatch(@Param("taskIds") List<UUID> taskIds,
                               @Param("assigneeId") UUID assigneeId,
                               @Param("updateTime") LocalDateTime updateTime);
    
    // Existence and validation queries
    
    /**
     * Checks if a task exists in a specific project
     */
    boolean existsByIdAndProjectId(UUID taskId, UUID projectId);
    
    /**
     * Checks if a user has any assigned tasks in a project
     */
    boolean existsByProjectIdAndAssigneeId(UUID projectId, UUID assigneeId);
    
    /**
     * Counts active tasks for a user
     */
    @Query("SELECT COUNT(t) FROM Task t " +
           "WHERE t.assignee.id = :userId AND t.status NOT IN :completedStatuses")
    long countActiveTasksForUser(@Param("userId") UUID userId,
                                @Param("completedStatuses") List<TaskStatus> completedStatuses);
    
    /**
     * Finds the most recent tasks for a project
     */
    @Query("SELECT t FROM Task t " +
           "WHERE t.project.id = :projectId " +
           "ORDER BY t.updatedAt DESC")
    List<Task> findRecentTasksForProject(@Param("projectId") UUID projectId, Pageable pageable);
    
    /**
     * Custom query to detect circular dependencies
     */
    @Query(value = """
        WITH RECURSIVE task_dependencies AS (
            SELECT task_id, depends_on_task_id, 1 as depth
            FROM task_dependencies td
            WHERE task_id = :taskId
            
            UNION ALL
            
            SELECT td.task_id, td.depends_on_task_id, tdr.depth + 1
            FROM task_dependencies td
            JOIN task_dependencies tdr ON td.task_id = tdr.depends_on_task_id
            WHERE tdr.depth < 10
        )
        SELECT COUNT(*) > 0 FROM task_dependencies 
        WHERE depends_on_task_id = :taskId
        """, nativeQuery = true)
    boolean hasCircularDependency(@Param("taskId") UUID taskId);
}