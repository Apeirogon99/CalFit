package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.billing.dashboard.PlanDistribution;
import java.util.List;

public record PlanDistributionResponse(List<PlanCountItem> distribution) {

  public record PlanCountItem(String status, long count) {}

  public static PlanDistributionResponse from(List<PlanDistribution> list) {
    return new PlanDistributionResponse(
        list.stream().map(p -> new PlanCountItem(p.status(), p.count())).toList());
  }
}
