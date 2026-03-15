package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.DashboardSummaryResponse;
import com.coffiness.calfit.api.v1.response.PlanDistributionResponse;
import com.coffiness.calfit.api.v1.response.RevenueCostTrendResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.billing.dashboard.DashboardService;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

  private final DashboardService dashboardService;

  @GetMapping("/summary")
  public ApiResponse<DashboardSummaryResponse> getSummary(
      @RequestParam(required = false) String month) {
    LocalDate date = parseMonth(month);
    return ApiResponse.success(
        DashboardSummaryResponse.from(
            dashboardService.getSummary(date.getYear(), date.getMonthValue())));
  }

  @GetMapping("/revenue-cost-trend")
  public ApiResponse<RevenueCostTrendResponse> getRevenueCostTrend() {
    return ApiResponse.success(
        RevenueCostTrendResponse.from(dashboardService.getRevenueCostTrend()));
  }

  @GetMapping("/plan-distribution")
  public ApiResponse<PlanDistributionResponse> getPlanDistribution() {
    return ApiResponse.success(
        PlanDistributionResponse.from(dashboardService.getPlanDistribution()));
  }

  private LocalDate parseMonth(String month) {
    if (month == null || month.isBlank()) {
      return LocalDate.now();
    }
    YearMonth ym = YearMonth.parse(month);
    return ym.atDay(1);
  }
}
