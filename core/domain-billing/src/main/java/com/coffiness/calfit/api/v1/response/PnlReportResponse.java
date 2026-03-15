package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.billing.pnl.PnlReport;
import java.util.List;

public record PnlReportResponse(
    List<String> columns,
    long[] revenueTotal,
    long[] laborCost,
    long[] infraCost,
    long[] marketingCost,
    long[] otherCost,
    long[] costTotal,
    long[] operatingProfit) {

  public static PnlReportResponse from(PnlReport report) {
    return new PnlReportResponse(
        report.columns(),
        report.revenueTotal(),
        report.laborCost(),
        report.infraCost(),
        report.marketingCost(),
        report.otherCost(),
        report.costTotal(),
        report.operatingProfit());
  }
}
