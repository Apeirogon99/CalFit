package com.coffiness.calfit.domain.payment.toss;

public record PaymentResponse(
    String paymentKey, String orderId, String status, Long totalAmount, String failureMessage) {}
