package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.domain.billing.cost.Cost;

public record CostResponse(
    Long id,
    CostCategory category,
    String name,
    Long amount,
    int costYear,
    int costMonth,
    String memo) {

  public static CostResponse from(Cost cost) {
    return new CostResponse(
        cost.id(),
        cost.category(),
        cost.name(),
        cost.amount(),
        cost.costYear(),
        cost.costMonth(),
        cost.memo());
  }
}
