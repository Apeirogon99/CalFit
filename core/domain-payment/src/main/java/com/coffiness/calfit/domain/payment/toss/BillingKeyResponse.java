package com.coffiness.calfit.domain.payment.toss;

public record BillingKeyResponse(
    String billingKey,
    String customerKey,
    String cardCompany,
    String cardNumber,
    String cardType,
    String ownerType) {}
