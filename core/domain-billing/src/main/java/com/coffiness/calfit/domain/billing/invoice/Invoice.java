package com.coffiness.calfit.domain.billing.invoice;

import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.core.enums.PlanType;
import java.time.LocalDate;

public record Invoice(
    Long id,
    Long subscriptionId,
    String workspaceId,
    PlanType planType,
    int billingYear,
    int billingMonth,
    Long amount,
    LocalDate issuedAt,
    LocalDate paidAt,
    InvoiceStatus invoiceStatus) {}
