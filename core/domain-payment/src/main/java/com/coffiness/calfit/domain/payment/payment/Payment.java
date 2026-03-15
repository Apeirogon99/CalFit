package com.coffiness.calfit.domain.payment.payment;

import com.coffiness.calfit.core.enums.PaymentStatus;
import java.time.LocalDateTime;

public record Payment(
    Long id,
    String workspaceId,
    Long subscriptionId,
    Long invoiceId,
    String billingKey,
    String paymentKey,
    String orderId,
    Long amount,
    PaymentStatus paymentStatus,
    String failReason,
    LocalDateTime paidAt,
    int retryCount,
    LocalDateTime nextRetryAt) {}
