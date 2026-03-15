package com.coffiness.calfit.domain.billing.invoice;

public record InvoiceSummary(
    long confirmedRevenue, long confirmedCount, long outstanding, long outstandingOverdue) {}
