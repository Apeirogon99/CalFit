package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.ApplicantLoginRequest;
import com.coffiness.calfit.api.v1.request.ApplicantSignUpRequest;
import com.coffiness.calfit.api.v1.response.ApplicantLoginResponse;
import com.coffiness.calfit.api.v1.response.ApplicantResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.applicant.ApplicantPortalService;
import com.coffiness.calfit.domain.applicant.ApplicantPortalService.ApplicantLoginResult;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicantPortalController {

  private final ApplicantPortalService applicantPortalService;
  private final WorkspaceRepository workspaceRepository;

  @PostMapping("/api/v1/workspaces/{workspaceId}/applicants/signup")
  public ApiResponse<ApplicantResponse> signUp(
      @PathVariable String workspaceId, @Valid @RequestBody ApplicantSignUpRequest request) {
    setTenantFromWorkspace(workspaceId);
    return ApiResponse.success(
        ApplicantResponse.from(
            applicantPortalService.signUp(request.email(), request.password(), request.name())));
  }

  @PostMapping("/api/v1/workspaces/{workspaceId}/applicants/login")
  public ApiResponse<ApplicantLoginResponse> login(
      @PathVariable String workspaceId, @Valid @RequestBody ApplicantLoginRequest request) {
    setTenantFromWorkspace(workspaceId);
    ApplicantLoginResult result = applicantPortalService.login(request.email(), request.password());
    return ApiResponse.success(
        new ApplicantLoginResponse(
            result.accessToken(), ApplicantResponse.from(result.applicant())));
  }

  private void setTenantFromWorkspace(String workspaceId) {
    if (workspaceId == null || workspaceId.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (workspaceRepository.findByWorkspaceId(workspaceId).isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }
    TenantContext.setTenantId(workspaceId);
  }
}
