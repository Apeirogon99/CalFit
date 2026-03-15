package com.coffiness.calfit.domain.billing.subscription;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Subscription(
    Long id,
    String workspaceId,
    PlanType planType,
    SubscriptionStatus subscriptionStatus,
    Long monthlyAmount,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime cancelledAt) {}
