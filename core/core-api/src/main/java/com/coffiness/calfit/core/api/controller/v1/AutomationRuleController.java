package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.AutomationRuleUpsertRequest;
import com.coffiness.calfit.api.v1.response.AutomationRuleResponse;
import com.coffiness.calfit.api.v1.response.AutomationTemplateResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.application.AutomationRuleService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AutomationRuleController {

  private final AutomationRuleService automationRuleService;
  private final MemberReader memberReader;

  @GetMapping("/api/v1/automation-templates")
  public ApiResponse<List<AutomationTemplateResponse>> getAutomationTemplates(
      @AuthenticationPrincipal SecurityUser user) {
    validateHrMember(user);
    return ApiResponse.success(automationRuleService.getTemplates());
  }

  @GetMapping("/api/v1/recruitments/{recruitmentId}/automation-rules")
  public ApiResponse<List<AutomationRuleResponse>> getAutomationRules(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long recruitmentId) {
    validateHrMember(user);
    return ApiResponse.success(automationRuleService.getRules(recruitmentId));
  }

  @PostMapping("/api/v1/recruitments/{recruitmentId}/automation-rules")
  public ApiResponse<AutomationRuleResponse> createAutomationRule(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long recruitmentId,
      @Valid @RequestBody AutomationRuleUpsertRequest request) {
    validateHrMember(user);
    return ApiResponse.success(automationRuleService.createRule(recruitmentId, request));
  }

  @PutMapping("/api/v1/automation-rules/{ruleId}")
  public ApiResponse<AutomationRuleResponse> updateAutomationRule(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long ruleId,
      @Valid @RequestBody AutomationRuleUpsertRequest request) {
    validateHrMember(user);
    return ApiResponse.success(automationRuleService.updateRule(ruleId, request));
  }

  @DeleteMapping("/api/v1/automation-rules/{ruleId}")
  public ApiResponse<Void> deleteAutomationRule(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long ruleId) {
    validateHrMember(user);
    automationRuleService.deleteRule(ruleId);
    return ApiResponse.success(null);
  }

  private Member validateHrMember(SecurityUser user) {
    if (user == null || "APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    Member member = memberReader.getMember(tenantId, user.userId());
    if (member == null || member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return member;
  }
}
