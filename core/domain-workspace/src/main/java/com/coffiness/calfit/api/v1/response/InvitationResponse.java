package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.invitation.Invitation;
import java.time.LocalDateTime;

public record InvitationResponse(
    String token, String email, MemberType memberType, LocalDateTime expiresAt) {

  public static InvitationResponse from(Invitation invitation) {
    return new InvitationResponse(
        invitation.token(), invitation.email(), invitation.memberType(), invitation.expiresAt());
  }
}
