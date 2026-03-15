package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.billing.pnl.PnlSummary;

public record PnlSummaryResponse(
    long totalRevenue,
    double revenueGrowth,
    long totalCost,
    double costGrowth,
    long operatingProfit,
    double operatingMargin) {

  public static PnlSummaryResponse from(PnlSummary summary) {
    return new PnlSummaryResponse(
        summary.totalRevenue(),
        summary.revenueGrowth(),
        summary.totalCost(),
        summary.costGrowth(),
        summary.operatingProfit(),
        summary.operatingMargin());
  }
}
