package com.coffiness.calfit.domain.billing.event;

import com.coffiness.calfit.domain.billing.subscription.SubscriptionStore;
import com.coffiness.calfit.domain.workspace.event.WorkspaceCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 워크스페이스 생성 이벤트를 수신해 Trial 구독을 자동 생성한다. */
@Component
@RequiredArgsConstructor
public class BillingEventHandler {

  private final SubscriptionStore subscriptionStore;

  @EventListener
  @Async
  public void onWorkspaceCreated(WorkspaceCreatedEvent event) {
    subscriptionStore.createTrialSubscription(event.workspaceId());
  }
}
