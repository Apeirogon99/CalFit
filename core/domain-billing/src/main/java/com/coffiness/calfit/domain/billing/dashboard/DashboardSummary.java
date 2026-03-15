package com.coffiness.calfit.domain.billing.dashboard;

public record DashboardSummary(
    long mrr,
    double mrrGrowth,
    long subscribers,
    long newThisMonth,
    double churnRate,
    double churnImprove,
    long totalCost,
    double costGrowth) {}
