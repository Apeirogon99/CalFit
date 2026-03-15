package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.InvoiceStatus;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.domain.billing.invoice.Invoice;
import java.time.LocalDate;

public record InvoiceResponse(
    Long id,
    String workspaceId,
    PlanType planType,
    String billingPeriod,
    Long amount,
    LocalDate issuedAt,
    LocalDate paidAt,
    InvoiceStatus invoiceStatus) {

  public static InvoiceResponse from(Invoice invoice) {
    return new InvoiceResponse(
        invoice.id(),
        invoice.workspaceId(),
        invoice.planType(),
        String.format("%d.%02d", invoice.billingYear(), invoice.billingMonth()),
        invoice.amount(),
        invoice.issuedAt(),
        invoice.paidAt(),
        invoice.invoiceStatus());
  }
}
