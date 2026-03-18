package com.coffiness.calfit.storage.db.core.billing;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity extends BaseEntity {

  @Column(nullable = false)
  private String workspaceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlanType planType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionStatus subscriptionStatus;

  @Column(nullable = false)
  private Long monthlyAmount;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column private LocalDate endDate;

  @Column private LocalDateTime cancelledAt;

  protected SubscriptionEntity() {}

  private SubscriptionEntity(
      String workspaceId,
      PlanType planType,
      SubscriptionStatus subscriptionStatus,
      Long monthlyAmount,
      LocalDate startDate) {
    this.workspaceId = workspaceId;
    this.planType = planType;
    this.subscriptionStatus = subscriptionStatus;
    this.monthlyAmount = monthlyAmount;
    this.startDate = startDate;
  }

  public static SubscriptionEntity createTrial(String workspaceId) {
    return new SubscriptionEntity(
        workspaceId, PlanType.BUSINESS, SubscriptionStatus.TRIAL, 0L, LocalDate.now());
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public PlanType getPlanType() {
    return planType;
  }

  public SubscriptionStatus getSubscriptionStatus() {
    return subscriptionStatus;
  }

  public Long getMonthlyAmount() {
    return monthlyAmount;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void updateStatus(SubscriptionStatus status) {
    this.subscriptionStatus = status;
  }

  public void cancel() {
    this.subscriptionStatus = SubscriptionStatus.CANCELLED;
    this.cancelledAt = LocalDateTime.now();
    this.endDate = LocalDate.now();
  }

  public void activate(PlanType planType, Long monthlyAmount) {
    this.planType = planType;
    this.subscriptionStatus = SubscriptionStatus.ACTIVE;
    this.monthlyAmount = monthlyAmount;
  }
}
