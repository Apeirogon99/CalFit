package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import java.time.LocalDate;

public record SubscriptionResponse(
    Long id,
    String workspaceId,
    PlanType planType,
    SubscriptionStatus subscriptionStatus,
    Long monthlyAmount,
    LocalDate startDate,
    LocalDate endDate) {

  public static SubscriptionResponse from(Subscription sub) {
    return new SubscriptionResponse(
        sub.id(),
        sub.workspaceId(),
        sub.planType(),
        sub.subscriptionStatus(),
        sub.monthlyAmount(),
        sub.startDate(),
        sub.endDate());
  }
}
