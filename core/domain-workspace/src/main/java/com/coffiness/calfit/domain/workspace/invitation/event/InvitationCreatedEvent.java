package com.coffiness.calfit.domain.workspace.invitation.event;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.support.event.DomainEvent;

/** 초대장 생성 이벤트 (추후 이메일 발송용) */
public record InvitationCreatedEvent(
    String workspaceId, String email, String token, MemberType memberType, long occurredAt)
    implements DomainEvent {

  public static InvitationCreatedEvent of(
      String workspaceId, String email, String token, MemberType memberType) {
    return new InvitationCreatedEvent(
        workspaceId, email, token, memberType, System.currentTimeMillis());
  }
}
