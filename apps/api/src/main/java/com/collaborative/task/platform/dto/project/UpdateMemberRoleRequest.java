package com.collaborative.task.platform.dto.project;

import com.collaborative.task.platform.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a project member's role.
 * Implements validation for role update requirements.
 */
public record UpdateMemberRoleRequest(
    @NotNull(message = "Role is required")
    ProjectRole role
) {}