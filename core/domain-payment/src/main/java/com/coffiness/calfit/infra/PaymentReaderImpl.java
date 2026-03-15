package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.PaymentStatus;
import com.coffiness.calfit.domain.payment.payment.Payment;
import com.coffiness.calfit.domain.payment.payment.PaymentReader;
import com.coffiness.calfit.storage.db.core.payment.PaymentEntity;
import com.coffiness.calfit.storage.db.core.payment.PaymentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReaderImpl implements PaymentReader {

  private final PaymentRepository paymentRepository;

  @Override
  public Optional<Payment> findById(Long id) {
    return paymentRepository.findById(id).map(this::toPayment);
  }

  @Override
  public List<Payment> findByWorkspaceId(String workspaceId, int page, int size) {
    return paymentRepository
        .findByWorkspaceIdAndStatusOrderByCreatedAtDesc(
            workspaceId, EntityStatus.ACTIVE, PageRequest.of(page, size))
        .stream()
        .map(this::toPayment)
        .toList();
  }

  @Override
  public long countByWorkspaceId(String workspaceId) {
    return paymentRepository.countByWorkspaceIdAndStatus(workspaceId, EntityStatus.ACTIVE);
  }

  @Override
  public List<Payment> findRetryablePayments() {
    return paymentRepository
        .findByPaymentStatusAndNextRetryAtBeforeAndStatus(
            PaymentStatus.RETRY, LocalDateTime.now(), EntityStatus.ACTIVE)
        .stream()
        .map(this::toPayment)
        .toList();
  }

  @Override
  public boolean existsSuccessfulPaymentInMonth(String workspaceId, int year, int month) {
    LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
    LocalDateTime end = start.plusMonths(1);
    return paymentRepository.existsByWorkspaceIdAndPaymentStatusAndPaidAtBetweenAndStatus(
        workspaceId, PaymentStatus.SUCCESS, start, end, EntityStatus.ACTIVE);
  }

  private Payment toPayment(PaymentEntity entity) {
    return new Payment(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getSubscriptionId(),
        entity.getInvoiceId(),
        entity.getBillingKey(),
        entity.getPaymentKey(),
        entity.getOrderId(),
        entity.getAmount(),
        entity.getPaymentStatus(),
        entity.getFailReason(),
        entity.getPaidAt(),
        entity.getRetryCount(),
        entity.getNextRetryAt());
  }
}
