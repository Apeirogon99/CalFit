package com.coffiness.calfit.domain.billing.subscription;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;

public interface SubscriptionStore {

  Subscription createTrialSubscription(String workspaceId);

  Subscription updateStatus(Long subscriptionId, SubscriptionStatus status);

  Subscription activate(Long subscriptionId, PlanType planType, Long monthlyAmount);

  void cancel(Long subscriptionId);
}
