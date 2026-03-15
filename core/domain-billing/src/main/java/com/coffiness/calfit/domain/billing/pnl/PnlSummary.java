package com.coffiness.calfit.domain.billing.pnl;

public record PnlSummary(
    long totalRevenue,
    double revenueGrowth,
    long totalCost,
    double costGrowth,
    long operatingProfit,
    double operatingMargin) {}
