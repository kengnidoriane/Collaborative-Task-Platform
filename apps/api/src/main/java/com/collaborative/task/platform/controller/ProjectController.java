package com.collaborative.task.platform.controller;

import com.collaborative.task.platform.dto.project.*;
import com.collaborative.task.platform.service.ProjectService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for project management operations.
 * Implements clean API design with comprehensive validation and error handling.
 */
@RestController
@RequestMapping("/api/v1/projects")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ProjectController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    
    private final ProjectService projectService;
    
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }
    
    /**
     * Create a new project.
     * POST /api/v1/projects
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication) {
        
        logger.info("Creating project '{}' for user {}", request.name(), authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        ProjectResponse project = projectService.createProject(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }
    
    /**
     * Get project by ID.
     * GET /api/v1/projects/{projectId}
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID projectId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        ProjectResponse project = projectService.getProject(projectId, userId);
        
        return ResponseEntity.ok(project);
    }
    
    /**
     * Update project information.
     * PUT /api/v1/projects/{projectId}
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication) {
        
        logger.info("Updating project {} by user {}", projectId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        ProjectResponse project = projectService.updateProject(projectId, request, userId);
        
        return ResponseEntity.ok(project);
    }
    
    /**
     * Delete project.
     * DELETE /api/v1/projects/{projectId}
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            Authentication authentication) {
        
        logger.info("Deleting project {} by user {}", projectId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        projectService.deleteProject(projectId, userId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get all projects accessible to the current user.
     * GET /api/v1/projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getUserProjects(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<ProjectResponse> projects = projectService.getUserProjects(userId);
        
        return ResponseEntity.ok(projects);
    }
    
    /**
     * Search projects by name.
     * GET /api/v1/projects/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProjectResponse>> searchProjects(
            @RequestParam("q") String searchTerm,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        List<ProjectResponse> projects = projectService.searchProjects(searchTerm, userId);
        
        return ResponseEntity.ok(projects);
    }
    
    /**
     * Get public projects for discovery.
     * GET /api/v1/projects/public
     */
    @GetMapping("/public")
    public ResponseEntity<Page<ProjectResponse>> getPublicProjects(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ProjectResponse> projects = projectService.getPublicProjects(pageable);
        return ResponseEntity.ok(projects);
    }
    
    /**
     * Invite a member to the project.
     * POST /api/v1/projects/{projectId}/members
     */
    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectMemberResponse> inviteMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication authentication) {
        
        logger.info("Inviting member {} to project {} by user {}", 
                   request.email(), projectId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        ProjectMemberResponse member = projectService.inviteMember(projectId, request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }
    
    /**
     * Accept project invitation.
     * POST /api/v1/projects/{projectId}/members/accept
     */
    @PostMapping("/{projectId}/members/accept")
    public ResponseEntity<ProjectMemberResponse> acceptInvitation(
            @PathVariable UUID projectId,
            Authentication authentication) {
        
        logger.info("User {} accepting invitation to project {}", authentication.getName(), projectId);
        
        UUID userId = UUID.fromString(authentication.getName());
        ProjectMemberResponse member = projectService.acceptInvitation(projectId, userId);
        
        return ResponseEntity.ok(member);
    }
    
    /**
     * Get project members.
     * GET /api/v1/projects/{projectId}/members
     */
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(
            @PathVariable UUID projectId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        List<ProjectMemberResponse> members = projectService.getProjectMembers(projectId, userId);
        
        return ResponseEntity.ok(members);
    }
    
    /**
     * Update member role.
     * PUT /api/v1/projects/{projectId}/members/{memberId}
     */
    @PutMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<ProjectMemberResponse> updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            Authentication authentication) {
        
        logger.info("Updating role for member {} in project {} by user {}", 
                   memberId, projectId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        ProjectMemberResponse member = projectService.updateMemberRole(projectId, memberId, request, userId);
        
        return ResponseEntity.ok(member);
    }
    
    /**
     * Remove member from project.
     * DELETE /api/v1/projects/{projectId}/members/{memberId}
     */
    @DeleteMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        
        logger.info("Removing member {} from project {} by user {}", 
                   memberId, projectId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        projectService.removeMember(projectId, memberId, userId);
        
        return ResponseEntity.noContent().build();
    }
}