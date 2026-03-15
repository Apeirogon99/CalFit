package com.coffiness.calfit.storage.db.core.billing;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

  Optional<SubscriptionEntity> findByWorkspaceIdAndStatus(String workspaceId, EntityStatus status);

  List<SubscriptionEntity> findByStatusOrderByCreatedAtDesc(EntityStatus status, Pageable pageable);

  @Query(
      "SELECT s FROM SubscriptionEntity s WHERE s.status = :entityStatus"
          + " AND (:subscriptionStatus IS NULL OR s.subscriptionStatus = :subscriptionStatus)"
          + " AND (:search IS NULL OR s.workspaceId LIKE %:search%)"
          + " ORDER BY s.createdAt DESC")
  List<SubscriptionEntity> findAllWithFilter(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus,
      @Param("search") String search,
      Pageable pageable);

  @Query(
      "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.status = :entityStatus"
          + " AND (:subscriptionStatus IS NULL OR s.subscriptionStatus = :subscriptionStatus)"
          + " AND (:search IS NULL OR s.workspaceId LIKE %:search%)")
  long countWithFilter(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus,
      @Param("search") String search);

  @Query(
      "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.status = :entityStatus AND s.subscriptionStatus = :subscriptionStatus")
  long countBySubscriptionStatus(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus);

  @Query(
      "SELECT COALESCE(SUM(s.monthlyAmount), 0) FROM SubscriptionEntity s WHERE s.status = :entityStatus AND s.subscriptionStatus = :subscriptionStatus")
  long sumMonthlyAmountBySubscriptionStatus(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus);

  List<SubscriptionEntity> findBySubscriptionStatusAndStatusOrderByCreatedAtDesc(
      SubscriptionStatus subscriptionStatus, EntityStatus entityStatus);

  List<SubscriptionEntity> findByPlanTypeAndSubscriptionStatusAndStatus(
      PlanType planType, SubscriptionStatus subscriptionStatus, EntityStatus entityStatus);

  @Query(
      "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.status = :entityStatus"
          + " AND s.subscriptionStatus = :subscriptionStatus"
          + " AND s.startDate <= :monthEnd"
          + " AND (s.endDate IS NULL OR s.endDate >= :monthStart)")
  long countByStatusInMonth(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus,
      @Param("monthStart") LocalDate monthStart,
      @Param("monthEnd") LocalDate monthEnd);

  @Query(
      "SELECT COALESCE(SUM(s.monthlyAmount), 0) FROM SubscriptionEntity s"
          + " WHERE s.status = :entityStatus"
          + " AND s.subscriptionStatus = :subscriptionStatus"
          + " AND s.startDate <= :monthEnd"
          + " AND (s.endDate IS NULL OR s.endDate >= :monthStart)")
  long sumMonthlyAmountByStatusInMonth(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus,
      @Param("monthStart") LocalDate monthStart,
      @Param("monthEnd") LocalDate monthEnd);

  @Query(
      "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.status = :entityStatus"
          + " AND s.startDate >= :monthStart AND s.startDate <= :monthEnd")
  long countNewInMonth(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("monthStart") LocalDate monthStart,
      @Param("monthEnd") LocalDate monthEnd);

  @Query(
      "SELECT COUNT(s) FROM SubscriptionEntity s WHERE s.status = :entityStatus"
          + " AND s.subscriptionStatus = 'CANCELLED'"
          + " AND s.cancelledAt >= :from AND s.cancelledAt < :to")
  long countCancelledInPeriod(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("from") java.time.LocalDateTime from,
      @Param("to") java.time.LocalDateTime to);
}
