package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.AutomationRuleUpsertRequest;
import com.coffiness.calfit.api.v1.response.AutomationRuleResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record AutomationRuleFixture(BaseFixture base) {

  public static AutomationRuleFixture create(Environment environment, ObjectMapper objectMapper) {
    return new AutomationRuleFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<AutomationRuleResponse> createRule(
      String token, String tenantId, Long recruitmentId, AutomationRuleUpsertRequest request) {
    String url = String.format("/api/v1/recruitments/%d/automation-rules", recruitmentId);
    return exchangeWithTenant(
        url, HttpMethod.POST, request, token, tenantId, AutomationRuleResponse.class);
  }

  public ApiResponse<List<AutomationRuleResponse>> getRules(
      String token, String tenantId, Long recruitmentId) {
    String url = String.format("/api/v1/recruitments/%d/automation-rules", recruitmentId);
    ApiResponse<AutomationRuleResponse[]> response =
        exchangeWithTenant(
            url, HttpMethod.GET, null, token, tenantId, AutomationRuleResponse[].class);
    return convertArrayResponse(response);
  }

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
        body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
    ResponseEntity<ApiResponse> response =
        base.client().exchange(url, method, entity, ApiResponse.class);
    return convertResponse(response.getBody(), responseType);
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> convertResponse(ApiResponse<?> response, Class<T> responseType) {
    if (response == null) {
      return null;
    }
    if (response.getResult() == ResultType.ERROR || response.getData() == null) {
      return (ApiResponse<T>) response;
    }
    if (responseType == Void.class) {
      return (ApiResponse<T>) response;
    }
    T converted = base.objectMapper().convertValue(response.getData(), responseType);
    return ApiResponse.success(converted);
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<List<T>> convertArrayResponse(ApiResponse<T[]> response) {
    if (response == null) {
      return null;
    }
    if (response.getResult() == ResultType.ERROR || response.getData() == null) {
      return (ApiResponse<List<T>>) (ApiResponse<?>) response;
    }
    return ApiResponse.success(List.of(response.getData()));
  }
}
