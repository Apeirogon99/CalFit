package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CreateWorkspaceRequest;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WorkspaceController {
  private final WorkspaceService workspaceService;
  private final MemberService memberService;

  @PostMapping("/api/v1/workspaces")
  public ApiResponse<WorkspaceResponse> createWorkspace(
      @Valid @RequestBody CreateWorkspaceRequest request,
      @AuthenticationPrincipal SecurityUser securityUser) {

    Workspace workspace =
        workspaceService.createWorkspace(
            request.name(), request.contactPhone(), request.employeeScale());

    TenantContext.setTenantId(workspace.workspaceId());

    memberService.registerMember(securityUser.userId(), MemberType.HR);

    return ApiResponse.success(WorkspaceResponse.from(workspace));
  }
}
