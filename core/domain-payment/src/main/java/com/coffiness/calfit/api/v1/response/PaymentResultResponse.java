package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.PaymentStatus;
import com.coffiness.calfit.domain.payment.payment.Payment;

public record PaymentResultResponse(
    Long paymentId, PaymentStatus status, Long amount, String failReason) {

  public static PaymentResultResponse from(Payment payment) {
    return new PaymentResultResponse(
        payment.id(), payment.paymentStatus(), payment.amount(), payment.failReason());
  }
}
