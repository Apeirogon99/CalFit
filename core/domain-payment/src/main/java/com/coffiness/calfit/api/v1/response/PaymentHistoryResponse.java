package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.PaymentStatus;
import com.coffiness.calfit.domain.payment.payment.Payment;
import java.time.LocalDateTime;

public record PaymentHistoryResponse(
    Long id, Long amount, PaymentStatus status, LocalDateTime paidAt, String orderId) {

  public static PaymentHistoryResponse from(Payment payment) {
    return new PaymentHistoryResponse(
        payment.id(),
        payment.amount(),
        payment.paymentStatus(),
        payment.paidAt(),
        payment.orderId());
  }
}
