package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.payment.payment.Payment;
import com.coffiness.calfit.domain.payment.payment.PaymentStore;
import com.coffiness.calfit.storage.db.core.payment.PaymentEntity;
import com.coffiness.calfit.storage.db.core.payment.PaymentRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentStoreImpl implements PaymentStore {

  private final PaymentRepository paymentRepository;

  @Override
  public Payment createPayment(
      String workspaceId,
      Long subscriptionId,
      Long invoiceId,
      String billingKey,
      String orderId,
      Long amount) {
    PaymentEntity entity =
        PaymentEntity.create(workspaceId, subscriptionId, invoiceId, billingKey, orderId, amount);
    return toPayment(paymentRepository.save(entity));
  }

  @Override
  public Payment markSuccess(Long paymentId, String paymentKey) {
    PaymentEntity entity =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.markSuccess(paymentKey);
    return toPayment(paymentRepository.save(entity));
  }

  @Override
  public Payment markFailed(Long paymentId, String failReason, LocalDateTime nextRetryAt) {
    PaymentEntity entity =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.markFailed(failReason, nextRetryAt);
    return toPayment(paymentRepository.save(entity));
  }

  @Override
  public Payment markFinalFailed(Long paymentId, String failReason) {
    PaymentEntity entity =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.markFinalFailed(failReason);
    return toPayment(paymentRepository.save(entity));
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
