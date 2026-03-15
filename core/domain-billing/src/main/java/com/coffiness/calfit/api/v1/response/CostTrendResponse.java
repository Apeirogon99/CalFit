package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.billing.cost.MonthlyCostTotal;
import java.util.List;

public record CostTrendResponse(List<MonthlyCostItem> trend) {

  public record MonthlyCostItem(int year, int month, long amount) {}

  public static CostTrendResponse from(List<MonthlyCostTotal> list) {
    return new CostTrendResponse(
        list.stream().map(c -> new MonthlyCostItem(c.year(), c.month(), c.amount())).toList());
  }
}
