package com.coffiness.calfit.domain.workspace.invitation;

import com.coffiness.calfit.core.enums.InvitationStatus;
import com.coffiness.calfit.core.enums.MemberType;
import java.time.LocalDateTime;

//
public record Invitation(
    Long id,
    String workspaceId,
    String email,
    String token,
    MemberType memberType,
    Long invitedBy,
    InvitationStatus status,
    LocalDateTime expiresAt) {}
