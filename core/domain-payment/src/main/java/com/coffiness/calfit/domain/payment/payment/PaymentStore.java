package com.coffiness.calfit.domain.payment.payment;

import java.time.LocalDateTime;

public interface PaymentStore {

  Payment createPayment(
      String workspaceId,
      Long subscriptionId,
      Long invoiceId,
      String billingKey,
      String orderId,
      Long amount);

  Payment markSuccess(Long paymentId, String paymentKey);

  Payment markFailed(Long paymentId, String failReason, LocalDateTime nextRetryAt);

  Payment markFinalFailed(Long paymentId, String failReason);
}
