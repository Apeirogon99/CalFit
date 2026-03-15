package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.billing.dashboard.RevenueCostTrend;
import java.util.List;

public record RevenueCostTrendResponse(List<MonthlyTrendItem> trend) {

  public record MonthlyTrendItem(int year, int month, long revenue, long cost) {}

  public static RevenueCostTrendResponse from(List<RevenueCostTrend> list) {
    return new RevenueCostTrendResponse(
        list.stream()
            .map(t -> new MonthlyTrendItem(t.year(), t.month(), t.revenue(), t.cost()))
            .toList());
  }
}
