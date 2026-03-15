package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.api.v1.request.ApplicationStageUpdateRequest;
import com.coffiness.calfit.api.v1.response.ApplicationCreateResponse;
import com.coffiness.calfit.api.v1.response.ApplicationDetailResponse;
import com.coffiness.calfit.api.v1.response.ApplicationSummaryResponse;
import com.coffiness.calfit.core.api.request.ApplicationProcessUpdateRequest;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.application.ApplicationService;
import com.coffiness.calfit.domain.application.KanbanBoard;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

  private final ApplicationService applicationService;
  private final RecruitmentRepository recruitmentRepository;
  private final MemberReader memberReader;

  @PostMapping("/api/v1/applications")
  public ApiResponse<ApplicationCreateResponse> createApplication(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody ApplicationCreateRequest request) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    setTenantFromRecruitment(request.recruitmentId());

    Long applicationId = applicationService.create(request, user.userId());
    return ApiResponse.success(new ApplicationCreateResponse(applicationId));
  }

  @GetMapping("/api/v1/applications")
  public ApiResponse<List<ApplicationSummaryResponse>> listApplications(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) Long recruitmentId,
      @RequestParam(required = false, defaultValue = "false") boolean all) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (all) {
      return ApiResponse.success(applicationService.listAll());
    }
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return ApiResponse.success(applicationService.listByRecruitment(recruitmentId));
  }

  @GetMapping("/api/v1/applications/{applicationId}")
  public ApiResponse<ApplicationDetailResponse> getApplicationDetail(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return ApiResponse.success(applicationService.getDetail(applicationId));
  }

  @PatchMapping("/api/v1/applications/{applicationId}/stage")
  public ApiResponse<ApplicationDetailResponse> updateApplicationStage(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long applicationId,
      @Valid @RequestBody ApplicationStageUpdateRequest request) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return ApiResponse.success(
        applicationService.updateStage(applicationId, request, user.userId()));
  }

  @GetMapping("/api/v1/applications/board")
  public ApiResponse<KanbanBoard> getKanbanBoard(
      @AuthenticationPrincipal SecurityUser user, @RequestParam Long recruitmentId) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return ApiResponse.success(applicationService.getKanbanBoard(recruitmentId));
  }

  @PatchMapping("/api/v1/applications/{applicationId}/process")
  public ApiResponse<Void> updateApplicationProcess(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long applicationId,
      @Valid @RequestBody ApplicationProcessUpdateRequest request) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if ("APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    validateHrMember(user.userId());
    applicationService.updateProcess(applicationId, request.recruitmentProcessId(), user.userId());
    return ApiResponse.success(null);
  }

  private void setTenantFromRecruitment(Long recruitmentId) {
    if (recruitmentId == null) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    String tenantId =
        recruitmentRepository
            .findTenantIdById(recruitmentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    TenantContext.setTenantId(tenantId);
  }

  private Member validateHrMember(Long userId) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || userId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    Member member = memberReader.getMember(tenantId, userId);
    if (member == null || member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    return member;
  }
}
