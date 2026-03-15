package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.RegisterBillingKeyRequest;
import com.coffiness.calfit.api.v1.response.CardInfoResponse;
import com.coffiness.calfit.api.v1.response.PaymentHistoryResponse;
import com.coffiness.calfit.api.v1.response.PaymentResultResponse;
import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
import com.coffiness.calfit.api.v1.response.TossConfigResponse;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

public record PaymentFixture(
    BaseFixture base, UserFixture userFixture, WorkspaceFixture workspaceFixture) {

  public static PaymentFixture create(Environment environment, ObjectMapper objectMapper) {
    BaseFixture base = BaseFixture.create(environment, objectMapper);
    UserFixture userFixture = UserFixture.create(environment, objectMapper);
    WorkspaceFixture workspaceFixture = WorkspaceFixture.create(environment, objectMapper);
    return new PaymentFixture(base, userFixture, workspaceFixture);
  }

  // ==================== Setup ====================

  /** 유저 생성 + 워크스페이스 생성 → PaymentContext 반환 */
  public PaymentContext setupWorkspace() {
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    return new PaymentContext(token, workspace.workspaceId());
  }

  // ==================== Payment API Calls ====================

  public ApiResponse<TossConfigResponse> getTossConfig(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/payments/toss-config",
        HttpMethod.GET,
        null,
        token,
        tenantId,
        TossConfigResponse.class);
  }

  public ApiResponse<CardInfoResponse> registerBillingKey(
      String token, String tenantId, String customerKey, String authKey) {
    RegisterBillingKeyRequest request = new RegisterBillingKeyRequest(customerKey, authKey);
    return exchangeWithTenant(
        "/api/v1/payments/billing-key",
        HttpMethod.POST,
        request,
        token,
        tenantId,
        CardInfoResponse.class);
  }

  public ApiResponse<CardInfoResponse> registerBillingKeyRaw(
      String token, String tenantId, Object rawRequest) {
    return exchangeWithTenant(
        "/api/v1/payments/billing-key",
        HttpMethod.POST,
        rawRequest,
        token,
        tenantId,
        CardInfoResponse.class);
  }

  public ApiResponse<PaymentResultResponse> upgrade(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/payments/upgrade",
        HttpMethod.POST,
        null,
        token,
        tenantId,
        PaymentResultResponse.class);
  }

  public ApiResponse<Void> downgrade(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/payments/downgrade", HttpMethod.POST, null, token, tenantId, Void.class);
  }

  public ApiResponse<CardInfoResponse> getCard(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/payments/card", HttpMethod.GET, null, token, tenantId, CardInfoResponse.class);
  }

  public ApiResponse<SubscriptionResponse> getSubscription(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/payments/subscription",
        HttpMethod.GET,
        null,
        token,
        tenantId,
        SubscriptionResponse.class);
  }

  public ApiResponse<PaymentHistoryResponse[]> getHistory(
      String token, String tenantId, int page, int size) {
    return exchangeWithTenant(
        "/api/v1/payments/history?page=" + page + "&size=" + size,
        HttpMethod.GET,
        null,
        token,
        tenantId,
        PaymentHistoryResponse[].class);
  }

  public ApiResponse<PaymentHistoryResponse[]> getHistory(String token, String tenantId) {
    return getHistory(token, tenantId, 0, 20);
  }

  // ==================== Private Helpers ====================

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> exchangeWithTenant(
      String url,
      HttpMethod method,
      Object body,
      String token,
      String tenantId,
      Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    if (tenantId != null && !tenantId.isBlank()) {
      headers.set("X-Tenant-ID", tenantId);
    }
    HttpEntity<?> entity =
        (body != null) ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

    ResponseEntity<ApiResponse> response =
        base.client().exchange(url, method, entity, ApiResponse.class);
    return convertResponse(response.getBody(), responseType);
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> convertResponse(ApiResponse<?> response, Class<T> responseType) {
    if (response == null) return null;
    if (response.getResult() == ResultType.ERROR || response.getData() == null) {
      return (ApiResponse<T>) response;
    }
    if (responseType == Void.class) return (ApiResponse<T>) response;
    T converted = base.objectMapper().convertValue(response.getData(), responseType);
    return ApiResponse.success(converted);
  }

  // ==================== Context Record ====================

  public record PaymentContext(String token, String workspaceId) {}
}
