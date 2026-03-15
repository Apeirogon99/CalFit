package com.coffiness.calfit.domain.payment.billingkey;

public record BillingKeyInfo(
    Long id,
    String workspaceId,
    String billingKey,
    String customerKey,
    String cardCompany,
    String cardNumber,
    String cardType,
    String ownerType,
    boolean active) {}
