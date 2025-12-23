package com.collaborative.task.platform.service;

import com.collaborative.task.platform.dto.project.*;
import com.collaborative.task.platform.entity.*;
import com.collaborative.task.platform.exception.AuthorizationException;
import com.collaborative.task.platform.exception.BusinessException;
import com.collaborative.task.platform.exception.ResourceNotFoundException;
import com.collaborative.task.platform.repository.ProjectMemberRepository;
import com.collaborative.task.platform.repository.ProjectRepository;
import com.collaborative.task.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for project management operations.
 * Implements clean business logic separation with comprehensive validation and security.
 */
@Service
@Transactional
public class ProjectService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public ProjectService(ProjectRepository projectRepository,
                         ProjectMemberRepository projectMemberRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }
    
    /**
     * Create a new project.
     * Requirement 2.1: WHEN a user creates a new project THEN the system SHALL establish the user as project owner.
     */
    public ProjectResponse createProject(CreateProjectRequest request, UUID ownerId) {
        logger.info("Creating new project '{}' for user {}", request.name(), ownerId);
        
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));
        
        // Validate project name uniqueness for user
        List<Project> existingProjects = projectRepository.findByOwnerAndArchivedFalseOrderByUpdatedAtDesc(owner);
        boolean nameExists = existingProjects.stream()
            .anyMatch(p -> p.getName().equalsIgnoreCase(request.name()));
        
        if (nameExists) {
            throw new BusinessException("Project with name '" + request.name() + "' already exists");
        }
        
        Project project = new Project(request.name(), request.description(), owner, request.isPrivate());
        Project savedProject = projectRepository.save(project);
        
        logger.info("Successfully created project {} with ID {}", savedProject.getName(), savedProject.getId());
        
        return ProjectResponse.from(savedProject, ProjectRole.OWNER);
    }
    
    /**
     * Get project by ID with user access validation.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UUID userId) {
        User user = getUserById(userId);
        Project project = getProjectWithAccessCheck(projectId, user);
        
        ProjectRole userRole = project.getMemberRole(user);
        return ProjectResponse.from(project, userRole);
    }
    
    /**
     * Update project information.
     * Requirement 2.3: WHEN a project owner modifies project settings THEN the system SHALL update configurations.
     */
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UUID userId) {
        logger.info("Updating project {} by user {}", projectId, userId);
        
        User user = getUserById(userId);
        Project project = getProjectWithAccessCheck(projectId, user);
        
        // Check if user has admin access
        if (!project.hasAdminAccess(user)) {
            throw new AuthorizationException("Only project admins can modify project settings");
        }
        
        if (!request.hasUpdates()) {
            throw new BusinessException("No updates provided");
        }
        
        // Apply updates
        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.isPrivate() != null) {
            project.setPrivate(request.isPrivate());
        }
        if (request.archived() != null) {
            project.setArchived(request.archived());
        }
        
        Project updatedProject = projectRepository.save(project);
        
        // Notify team members of settings change
        notificationService.notifyProjectSettingsChanged(updatedProject, user);
        
        logger.info("Successfully updated project {}", projectId);
        
        ProjectRole userRole = updatedProject.getMemberRole(user);
        return ProjectResponse.from(updatedProject, userRole);
    }
    
    /**
     * Get all projects accessible to a user.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(UUID userId) {
        User user = getUserById(userId);
        List<Project> projects = projectRepository.findAccessibleProjectsByUser(user);
        
        return projects.stream()
            .map(project -> {
                ProjectRole userRole = project.getMemberRole(user);
                return ProjectResponse.from(project, userRole);
            })
            .toList();
    }
    
    /**
     * Search projects by name.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchProjects(String searchTerm, UUID userId) {
        User user = getUserById(userId);
        List<Project> projects = projectRepository.findProjectsByNameContaining(searchTerm, user);
        
        return projects.stream()
            .map(project -> {
                ProjectRole userRole = project.getMemberRole(user);
                return ProjectResponse.from(project, userRole);
            })
            .toList();
    }
    
    /**
     * Get public projects for discovery.
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getPublicProjects(Pageable pageable) {
        Page<Project> projects = projectRepository.findByIsPrivateFalseAndArchivedFalseOrderByUpdatedAtDesc(pageable);
        return projects.map(ProjectResponse::from);
    }
    
    /**
     * Invite a member to the project.
     * Requirement 2.2: WHEN a project owner invites team members THEN the system SHALL send invitations.
     */
    public ProjectMemberResponse inviteMember(UUID projectId, InviteMemberRequest request, UUID inviterId) {
        logger.info("Inviting member {} to project {} by user {}", request.email(), projectId, inviterId);
        
        User inviter = getUserById(inviterId);
        Project project = getProjectWithAccessCheck(projectId, inviter);
        
        // Check if inviter has permission to invite members
        if (!project.hasAdminAccess(inviter)) {
            throw new AuthorizationException("Only project admins can invite members");
        }
        
        // Find user to invite
        User invitee = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));
        
        // Check if user is already a member
        Optional<ProjectMember> existingMember = projectMemberRepository.findByProjectAndUser(project, invitee);
        if (existingMember.isPresent()) {
            if (existingMember.get().isPendingInvitation()) {
                throw new BusinessException("User already has a pending invitation to this project");
            } else {
                throw new BusinessException("User is already a member of this project");
            }
        }
        
        // Validate role assignment
        if (request.role() == ProjectRole.OWNER) {
            throw new BusinessException("Cannot invite user as owner. Transfer ownership instead.");
        }
        
        // Create invitation
        ProjectMember invitation = new ProjectMember(project, invitee, request.role(), inviter.getId());
        ProjectMember savedInvitation = projectMemberRepository.save(invitation);
        
        // Send notification
        notificationService.notifyProjectInvitation(project, invitee, inviter);
        
        logger.info("Successfully invited {} to project {}", request.email(), projectId);
        
        return ProjectMemberResponse.from(savedInvitation);
    }
    
    /**
     * Accept project invitation.
     */
    public ProjectMemberResponse acceptInvitation(UUID projectId, UUID userId) {
        logger.info("User {} accepting invitation to project {}", userId, projectId);
        
        User user = getUserById(userId);
        Project project = getProjectById(projectId);
        
        ProjectMember invitation = projectMemberRepository.findByProjectAndUser(project, user)
            .orElseThrow(() -> new ResourceNotFoundException("No invitation found for this project"));
        
        if (!invitation.isPendingInvitation()) {
            throw new BusinessException("Invitation has already been accepted");
        }
        
        invitation.acceptInvitation();
        ProjectMember acceptedMember = projectMemberRepository.save(invitation);
        
        // Notify project admins
        notificationService.notifyInvitationAccepted(project, user);
        
        logger.info("User {} successfully joined project {}", userId, projectId);
        
        return ProjectMemberResponse.from(acceptedMember);
    }
    
    /**
     * Get project members.
     */
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(UUID projectId, UUID userId) {
        User user = getUserById(userId);
        Project project = getProjectWithAccessCheck(projectId, user);
        
        List<ProjectMember> members = projectMemberRepository.findByProjectOrderByJoinedAtAsc(project);
        return members.stream()
            .map(ProjectMemberResponse::from)
            .toList();
    }
    
    /**
     * Update member role.
     */
    public ProjectMemberResponse updateMemberRole(UUID projectId, UUID memberId, 
                                                UpdateMemberRoleRequest request, UUID adminId) {
        logger.info("Updating role for member {} in project {} by admin {}", memberId, projectId, adminId);
        
        User admin = getUserById(adminId);
        Project project = getProjectWithAccessCheck(projectId, admin);
        
        // Check admin permissions
        if (!project.hasAdminAccess(admin)) {
            throw new AuthorizationException("Only project admins can update member roles");
        }
        
        User member = getUserById(memberId);
        ProjectMember projectMember = projectMemberRepository.findByProjectAndUser(project, member)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found in project"));
        
        // Validate role change
        if (request.role() == ProjectRole.OWNER) {
            throw new BusinessException("Cannot assign owner role. Use transfer ownership instead.");
        }
        
        if (project.isOwner(member)) {
            throw new BusinessException("Cannot change role of project owner");
        }
        
        projectMember.setRole(request.role());
        ProjectMember updatedMember = projectMemberRepository.save(projectMember);
        
        // Notify member of role change
        notificationService.notifyRoleChanged(project, member, request.role(), admin);
        
        logger.info("Successfully updated role for member {} in project {}", memberId, projectId);
        
        return ProjectMemberResponse.from(updatedMember);
    }
    
    /**
     * Remove member from project.
     * Requirement 2.4: WHEN a project owner removes a team member THEN the system SHALL revoke access.
     */
    public void removeMember(UUID projectId, UUID memberId, UUID adminId) {
        logger.info("Removing member {} from project {} by admin {}", memberId, projectId, adminId);
        
        User admin = getUserById(adminId);
        Project project = getProjectWithAccessCheck(projectId, admin);
        
        // Check admin permissions
        if (!project.hasAdminAccess(admin)) {
            throw new AuthorizationException("Only project admins can remove members");
        }
        
        User member = getUserById(memberId);
        
        // Cannot remove project owner
        if (project.isOwner(member)) {
            throw new BusinessException("Cannot remove project owner. Transfer ownership first.");
        }
        
        ProjectMember projectMember = projectMemberRepository.findByProjectAndUser(project, member)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found in project"));
        
        projectMemberRepository.delete(projectMember);
        
        // Notify member of removal
        notificationService.notifyMemberRemoved(project, member, admin);
        
        logger.info("Successfully removed member {} from project {}", memberId, projectId);
    }
    
    /**
     * Delete project.
     * Requirement 2.5: WHERE project deletion is requested THEN the system SHALL require confirmation.
     */
    public void deleteProject(UUID projectId, UUID ownerId) {
        logger.info("Deleting project {} by owner {}", projectId, ownerId);
        
        User owner = getUserById(ownerId);
        Project project = getProjectWithAccessCheck(projectId, owner);
        
        // Only owner can delete project
        if (!project.isOwner(owner)) {
            throw new AuthorizationException("Only project owner can delete the project");
        }
        
        // Delete all members first
        projectMemberRepository.deleteByProject(project);
        
        // Delete project
        projectRepository.delete(project);
        
        logger.info("Successfully deleted project {}", projectId);
    }
    
    // Helper methods
    
    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
    
    private Project getProjectById(UUID projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
    }
    
    private Project getProjectWithAccessCheck(UUID projectId, User user) {
        Project project = getProjectById(projectId);
        
        if (!projectRepository.hasUserAccessToProject(projectId, user)) {
            throw new AuthorizationException("Access denied to project: " + projectId);
        }
        
        return project;
    }
}