package com.coffiness.calfit.storage.db.core.payment;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

  List<PaymentEntity> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(
      String workspaceId, EntityStatus status, Pageable pageable);

  List<PaymentEntity> findByPaymentStatusAndNextRetryAtBeforeAndStatus(
      PaymentStatus paymentStatus, LocalDateTime now, EntityStatus status);

  long countByWorkspaceIdAndStatus(String workspaceId, EntityStatus status);

  boolean existsByWorkspaceIdAndPaymentStatusAndPaidAtBetweenAndStatus(
      String workspaceId,
      PaymentStatus paymentStatus,
      LocalDateTime paidAtStart,
      LocalDateTime paidAtEnd,
      EntityStatus status);
}
