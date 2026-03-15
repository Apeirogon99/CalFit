package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.PnlReportResponse;
import com.coffiness.calfit.api.v1.response.PnlSummaryResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.billing.pnl.PnlService;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/pnl")
@RequiredArgsConstructor
public class AdminPnlController {

  private final PnlService pnlService;

  @GetMapping("/summary")
  public ApiResponse<PnlSummaryResponse> getSummary(@RequestParam(required = false) String month) {
    LocalDate date = parseMonth(month);
    return ApiResponse.success(
        PnlSummaryResponse.from(pnlService.getSummary(date.getYear(), date.getMonthValue())));
  }

  @GetMapping("/report")
  public ApiResponse<PnlReportResponse> getReport(@RequestParam(defaultValue = "3") int months) {
    return ApiResponse.success(PnlReportResponse.from(pnlService.getReport(months)));
  }

  private LocalDate parseMonth(String month) {
    if (month == null || month.isBlank()) {
      return LocalDate.now();
    }
    YearMonth ym = YearMonth.parse(month);
    return ym.atDay(1);
  }
}
