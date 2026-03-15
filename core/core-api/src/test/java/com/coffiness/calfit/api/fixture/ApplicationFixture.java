package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.ApplicationCreateRequest;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.application.KanbanBoard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record ApplicationFixture(BaseFixture base) {

  public static ApplicationFixture create(Environment environment, ObjectMapper objectMapper) {
    return new ApplicationFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<Void> createApplication(String token, ApplicationCreateRequest request) {
    return base.post("/api/v1/applications", request, token, Void.class);
  }

  public ApiResponse<KanbanBoard> getKanbanBoard(
      String token, String tenantId, Long recruitmentId) {
    String url = String.format("/api/v1/applications/board?recruitmentId=%s", recruitmentId);
    return exchangeWithTenant(url, token, tenantId, KanbanBoard.class);
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> exchangeWithTenant(
      String url, String token, String tenantId, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    if (tenantId != null && !tenantId.isBlank()) {
      headers.set("X-Tenant-ID", tenantId);
    }
    HttpEntity<?> entity = new HttpEntity<>(headers);
    ResponseEntity<ApiResponse> response =
        base.client().exchange(url, HttpMethod.GET, entity, ApiResponse.class);
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
}
