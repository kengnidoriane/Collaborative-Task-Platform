package com.collaborative.task.platform.dto.project;

import com.collaborative.task.platform.entity.ProjectMember;
import com.collaborative.task.platform.entity.ProjectRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for project member information.
 * Provides clean API response format for project membership data.
 */
public record ProjectMemberResponse(
    UUID userId,
    String fullName,
    String email,
    String avatarUrl,
    ProjectRole role,
    LocalDateTime joinedAt,
    UUID invitedBy,
    boolean isPending,
    LocalDateTime invitationAcceptedAt
) {
    
    /**
     * Create ProjectMemberResponse from ProjectMember entity.
     */
    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
            member.getUser().getId(),
            member.getUser().getFullName(),
            member.getUser().getEmail(),
            member.getUser().getAvatarUrl(),
            member.getRole(),
            member.getJoinedAt(),
            member.getInvitedBy(),
            member.isPendingInvitation(),
            member.getInvitationAcceptedAt()
        );
    }
}