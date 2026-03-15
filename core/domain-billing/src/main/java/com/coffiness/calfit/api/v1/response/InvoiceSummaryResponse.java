package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.billing.invoice.InvoiceSummary;

public record InvoiceSummaryResponse(
    long confirmedRevenue, long confirmedCount, long outstanding, long outstandingOverdue) {

  public static InvoiceSummaryResponse from(InvoiceSummary summary) {
    return new InvoiceSummaryResponse(
        summary.confirmedRevenue(),
        summary.confirmedCount(),
        summary.outstanding(),
        summary.outstandingOverdue());
  }
}
