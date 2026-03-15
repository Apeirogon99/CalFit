package com.coffiness.calfit.domain.workspace.event;

import com.coffiness.calfit.support.event.DomainEvent;

/** 워크스페이스 생성 이벤트 — billing 모듈에서 Trial 구독 생성 트리거 */
public record WorkspaceCreatedEvent(String workspaceId, long occurredAt) implements DomainEvent {

  public static WorkspaceCreatedEvent of(String workspaceId) {
    return new WorkspaceCreatedEvent(workspaceId, System.currentTimeMillis());
  }
}
