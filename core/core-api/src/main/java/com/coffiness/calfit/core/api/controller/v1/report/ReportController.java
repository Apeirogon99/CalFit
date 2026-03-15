package com.coffiness.calfit.core.api.controller.v1.report;

import com.coffiness.calfit.core.api.facade.report.ReportFacade;
import com.coffiness.calfit.core.api.facade.report.ReportFacade.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportFacade reportFacade;

  @GetMapping("/kpi")
  public ApiResponse<KpiResult> getKpi() {
    return ApiResponse.success(reportFacade.getKpi());
  }

  @GetMapping("/pipeline")
  public ApiResponse<PipelineResult> getPipeline(@RequestParam(defaultValue = "6") int months) {
    return ApiResponse.success(reportFacade.getPipeline(months));
  }

  @GetMapping("/recruitment-performance")
  public ApiResponse<List<RecruitmentPerformanceItem>> getRecruitmentPerformance() {
    return ApiResponse.success(reportFacade.getRecruitmentPerformance());
  }

  @GetMapping("/interview")
  public ApiResponse<InterviewReportResult> getInterviewReport() {
    return ApiResponse.success(reportFacade.getInterviewReport());
  }

  @GetMapping("/automation")
  public ApiResponse<AutomationReportResult> getAutomationReport() {
    return ApiResponse.success(reportFacade.getAutomationReport());
  }
}
