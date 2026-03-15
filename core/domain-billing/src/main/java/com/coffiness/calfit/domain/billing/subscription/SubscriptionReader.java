package com.coffiness.calfit.domain.billing.subscription;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface SubscriptionReader {

  Optional<Subscription> findById(Long id);

  Optional<Subscription> findByWorkspaceId(String workspaceId);

  List<Subscription> getAll(SubscriptionStatus status, String search, Pageable pageable);

  long countAll(SubscriptionStatus status, String search);

  long countByStatus(SubscriptionStatus status);

  long getMrr();

  long countByStatusInMonth(SubscriptionStatus status, LocalDate monthStart, LocalDate monthEnd);

  long getMrrInMonth(LocalDate monthStart, LocalDate monthEnd);

  List<Subscription> findActiveByPlanType(PlanType planType);

  long countNewInMonth(LocalDate monthStart, LocalDate monthEnd);

  long countCancelledInMonth(int year, int month);
}
