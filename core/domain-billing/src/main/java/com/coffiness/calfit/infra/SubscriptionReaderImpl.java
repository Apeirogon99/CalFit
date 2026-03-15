package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionReader;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionEntity;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionReaderImpl implements SubscriptionReader {

  private final SubscriptionRepository subscriptionRepository;

  @Override
  public Optional<Subscription> findById(Long id) {
    return subscriptionRepository.findById(id).map(this::toSubscription);
  }

  @Override
  public Optional<Subscription> findByWorkspaceId(String workspaceId) {
    return subscriptionRepository
        .findByWorkspaceIdAndStatus(workspaceId, EntityStatus.ACTIVE)
        .map(this::toSubscription);
  }

  @Override
  public List<Subscription> getAll(SubscriptionStatus status, String search, Pageable pageable) {
    String searchParam = (search != null && !search.isBlank()) ? search : null;
    return subscriptionRepository
        .findAllWithFilter(EntityStatus.ACTIVE, status, searchParam, pageable)
        .stream()
        .map(this::toSubscription)
        .toList();
  }

  @Override
  public long countAll(SubscriptionStatus status, String search) {
    String searchParam = (search != null && !search.isBlank()) ? search : null;
    return subscriptionRepository.countWithFilter(EntityStatus.ACTIVE, status, searchParam);
  }

  @Override
  public long countByStatus(SubscriptionStatus status) {
    return subscriptionRepository.countBySubscriptionStatus(EntityStatus.ACTIVE, status);
  }

  @Override
  public long getMrr() {
    return subscriptionRepository.sumMonthlyAmountBySubscriptionStatus(
        EntityStatus.ACTIVE, SubscriptionStatus.ACTIVE);
  }

  @Override
  public long countByStatusInMonth(
      SubscriptionStatus status, LocalDate monthStart, LocalDate monthEnd) {
    return subscriptionRepository.countByStatusInMonth(
        EntityStatus.ACTIVE, status, monthStart, monthEnd);
  }

  @Override
  public long getMrrInMonth(LocalDate monthStart, LocalDate monthEnd) {
    return subscriptionRepository.sumMonthlyAmountByStatusInMonth(
        EntityStatus.ACTIVE, SubscriptionStatus.ACTIVE, monthStart, monthEnd);
  }

  @Override
  public List<Subscription> findActiveByPlanType(PlanType planType) {
    return subscriptionRepository
        .findByPlanTypeAndSubscriptionStatusAndStatus(
            planType, SubscriptionStatus.ACTIVE, EntityStatus.ACTIVE)
        .stream()
        .map(this::toSubscription)
        .toList();
  }

  @Override
  public long countNewInMonth(LocalDate monthStart, LocalDate monthEnd) {
    return subscriptionRepository.countNewInMonth(EntityStatus.ACTIVE, monthStart, monthEnd);
  }

  @Override
  public long countCancelledInMonth(int year, int month) {
    LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
    LocalDateTime to = from.plusMonths(1);
    return subscriptionRepository.countCancelledInPeriod(EntityStatus.ACTIVE, from, to);
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
