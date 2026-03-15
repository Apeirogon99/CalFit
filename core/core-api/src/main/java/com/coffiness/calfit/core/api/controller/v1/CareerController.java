package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.CareerApplyFormResponse;
import com.coffiness.calfit.api.v1.response.CareerCompnayResponse;
import com.coffiness.calfit.api.v1.response.CareerRecruitmentResponse;
import com.coffiness.calfit.core.api.facade.recruitment.CareerFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CareerController {
  private final CareerFacade careerFacade;

  @GetMapping("/api/v1/careers")
  public ApiResponse<List<CareerCompnayResponse>> getCompanies(
      @RequestParam(required = false) String search) {
    return ApiResponse.success(careerFacade.getCompanies(search));
  }

  @GetMapping("/api/v1/careers/{workspaceId}")
  public ApiResponse<List<CareerRecruitmentResponse>> getRecruitments(
      @PathVariable String workspaceId, @RequestParam(required = false) String search) {
    return ApiResponse.success(careerFacade.getRecruitments(workspaceId, search));
  }

  @GetMapping("/api/v1/careers/{workspaceId}/recruitments/{recruitmentId}/apply-form")
  public ApiResponse<CareerApplyFormResponse> getApplyForm(
      @PathVariable String workspaceId, @PathVariable Long recruitmentId) {
    return ApiResponse.success(careerFacade.getApplyForm(workspaceId, recruitmentId));
  }
}
