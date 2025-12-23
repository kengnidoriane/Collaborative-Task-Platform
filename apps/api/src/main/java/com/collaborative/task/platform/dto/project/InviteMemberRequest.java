package com.collaborative.task.platform.dto.project;

import com.collaborative.task.platform.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for inviting a member to a project.
 * Implements validation for team member invitation requirements.
 */
public record InviteMemberRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotNull(message = "Role is required")
    ProjectRole role
) {
    
    /**
     * Constructor with default MEMBER role.
     */
    public InviteMemberRequest(String email) {
        this(email, ProjectRole.MEMBER);
    }
}