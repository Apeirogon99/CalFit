package com.coffiness.calfit.core.api.controller.v1.dashboard;

import com.coffiness.calfit.api.v1.request.DashboardChatMessageCreateRequest;
import com.coffiness.calfit.api.v1.response.DashboardChatMessageResponse;
import com.coffiness.calfit.core.api.facade.dashboard.DashboardChatFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/*
 * 워크스페이스 대시보드의 익명 채팅 API를 제공
 * */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard-chat")
public class DashboardChatController {

  private final DashboardChatFacade dashboardChatFacade;

  @GetMapping("/messages")
  public ApiResponse<List<DashboardChatMessageResponse>> list(
      @AuthenticationPrincipal SecurityUser user) {
    return ApiResponse.success(
        dashboardChatFacade.getRecentMessages(currentTenantId(), user.userId()));
  }

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/messages")
  public ApiResponse<DashboardChatMessageResponse> create(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody DashboardChatMessageCreateRequest request) {
    return ApiResponse.success(
        dashboardChatFacade.createMessage(currentTenantId(), user.userId(), request.content()));
  }

  // 요청 헤더의 tenant 정보를 현재 워크스페이스 식별자로 사용
  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("Tenant ID is required.");
    }
    return tenantId;
  }
}
