package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionStore;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionEntity;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionStoreImpl implements SubscriptionStore {

  private final SubscriptionRepository subscriptionRepository;

  @Override
  public Subscription createTrialSubscription(String workspaceId) {
    SubscriptionEntity entity = SubscriptionEntity.createTrial(workspaceId);
    SubscriptionEntity saved = subscriptionRepository.save(entity);
    return toSubscription(saved);
  }

  @Override
  public Subscription updateStatus(Long subscriptionId, SubscriptionStatus status) {
    SubscriptionEntity entity =
        subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.updateStatus(status);
    return toSubscription(subscriptionRepository.save(entity));
  }

  @Override
  public Subscription activate(Long subscriptionId, PlanType planType, Long monthlyAmount) {
    SubscriptionEntity entity =
        subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.activate(planType, monthlyAmount);
    return toSubscription(subscriptionRepository.save(entity));
  }

  @Override
  public void cancel(Long subscriptionId) {
    SubscriptionEntity entity =
        subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.cancel();
    subscriptionRepository.save(entity);
  }

  private Subscription toSubscription(SubscriptionEntity entity) {
    return new Subscription(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getPlanType(),
        entity.getSubscriptionStatus(),
        entity.getMonthlyAmount(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCancelledAt());
  }
}
