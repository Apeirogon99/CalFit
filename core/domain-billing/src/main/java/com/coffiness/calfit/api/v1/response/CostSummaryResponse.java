package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CostCategory;
import java.util.Map;

public record CostSummaryResponse(Map<CostCategory, Long> byCategory, long total) {

  public static CostSummaryResponse from(Map<CostCategory, Long> summary) {
    long total = summary.values().stream().mapToLong(Long::longValue).sum();
    return new CostSummaryResponse(summary, total);
  }
}
