package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CreateCostRequest;
import com.coffiness.calfit.api.v1.request.UpdateCostRequest;
import com.coffiness.calfit.api.v1.response.CostResponse;
import com.coffiness.calfit.api.v1.response.CostSummaryResponse;
import com.coffiness.calfit.api.v1.response.CostTrendResponse;
import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.billing.cost.Cost;
import com.coffiness.calfit.domain.billing.cost.CostService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/costs")
@RequiredArgsConstructor
public class AdminCostController {

  private final CostService costService;

  @GetMapping("/summary")
  public ApiResponse<CostSummaryResponse> getSummary(@RequestParam(required = false) String month) {
    LocalDate date = parseMonth(month);
    return ApiResponse.success(
        CostSummaryResponse.from(
            costService.getSummaryByCategory(date.getYear(), date.getMonthValue())));
  }

  @GetMapping("/trend")
  public ApiResponse<CostTrendResponse> getTrend() {
    return ApiResponse.success(CostTrendResponse.from(costService.getMonthlyTrend()));
  }

  @GetMapping
  public ApiResponse<List<CostResponse>> getCosts(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    CostCategory cat = parseCategory(category);
    List<Cost> costs = costService.getAll(year, month, cat, page, size);
    return ApiResponse.success(costs.stream().map(CostResponse::from).toList());
  }

  @PostMapping
  public ApiResponse<CostResponse> createCost(@Valid @RequestBody CreateCostRequest request) {
    Cost cost =
        costService.create(
            request.category(),
            request.name(),
            request.amount(),
            request.year(),
            request.month(),
            request.memo());
    return ApiResponse.success(CostResponse.from(cost));
  }

  @PutMapping("/{id}")
  public ApiResponse<CostResponse> updateCost(
      @PathVariable Long id, @Valid @RequestBody UpdateCostRequest request) {
    Cost cost =
        costService.update(
            id,
            request.category(),
            request.name(),
            request.amount(),
            request.year(),
            request.month(),
            request.memo());
    return ApiResponse.success(CostResponse.from(cost));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<?> deleteCost(@PathVariable Long id) {
    costService.delete(id);
    return ApiResponse.success();
  }

  private LocalDate parseMonth(String month) {
    if (month == null || month.isBlank()) {
      return LocalDate.now();
    }
    YearMonth ym = YearMonth.parse(month);
    return ym.atDay(1);
  }

  private CostCategory parseCategory(String category) {
    if (category == null || category.isBlank()) return null;
    try {
      return CostCategory.valueOf(category.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
