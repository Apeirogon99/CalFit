package com.coffiness.calfit.domain.billing.cost;

import com.coffiness.calfit.core.enums.CostCategory;

public record Cost(
    Long id,
    CostCategory category,
    String name,
    Long amount,
    int costYear,
    int costMonth,
    String memo) {}
