package com.coffiness.calfit.core.api.config;

import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

  private static final String DEFAULT_TENANT = "default";

  private final MemberRepository memberRepository;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String tenantId = request.getHeader("X-Tenant-ID");

    if (tenantId != null && !tenantId.isBlank()) {
      TenantContext.setTenantId(tenantId);
      return true;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof SecurityUser user) {
      memberRepository
          .findTenantIdByUserId(user.userId())
          .ifPresentOrElse(
              TenantContext::setTenantId, () -> TenantContext.setTenantId(DEFAULT_TENANT));
      return true;
    }

    TenantContext.setTenantId(DEFAULT_TENANT);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    TenantContext.clear();
  }
}
