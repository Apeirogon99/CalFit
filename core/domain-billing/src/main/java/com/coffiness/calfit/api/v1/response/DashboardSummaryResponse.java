package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.billing.dashboard.DashboardSummary;

public record DashboardSummaryResponse(
    long mrr,
    double mrrGrowth,
    long subscribers,
    long newThisMonth,
    double churnRate,
    double churnImprove,
    long totalCost,
    double costGrowth) {

  public static DashboardSummaryResponse from(DashboardSummary summary) {
    return new DashboardSummaryResponse(
        summary.mrr(),
        summary.mrrGrowth(),
        summary.subscribers(),
        summary.newThisMonth(),
        summary.churnRate(),
        summary.churnImprove(),
        summary.totalCost(),
        summary.costGrowth());
  }
}
