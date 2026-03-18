package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.response.ApplicationTemplateListResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record ApplicationTemplateFixture(BaseFixture base) {

  public static ApplicationTemplateFixture create(
      Environment environment, ObjectMapper objectMapper) {
    return new ApplicationTemplateFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<List<ApplicationTemplateListResponse>> list(String token, String tenantId) {
    ApiResponse<ApplicationTemplateListResponse[]> response =
        exchangeWithTenant(
            "/api/v1/workspaces/{workspaceId}/application-templates",
            HttpMethod.GET,
            null,
            token,
            tenantId,
            ApplicationTemplateListResponse[].class,
            tenantId);

    return convertArrayResponse(response);
  }

  public ApiResponse<ApplicationTemplateListResponse> create(
      String token, String tenantId, Object request) {
    return exchangeWithTenant(
        "/api/v1/workspaces/{workspaceId}/application-templates",
        HttpMethod.POST,
        request,
        token,
        tenantId,
        ApplicationTemplateListResponse.class,
        tenantId);
  }

  public ApiResponse<ApplicationTemplateListResponse> updateStatus(
      String token, String tenantId, Long templateId, Object request) {
    return exchangeWithTenant(
        "/api/v1/workspaces/{workspaceId}/application-templates/{templateId}/status",
        HttpMethod.PATCH,
        request,
        token,
        tenantId,
        ApplicationTemplateListResponse.class,
        tenantId,
        templateId);
  }

  public Long createUsedTemplateId(String token, String tenantId, String namePrefix) {
    String templateName = namePrefix + "-" + System.nanoTime();
    ApiResponse<ApplicationTemplateListResponse> response =
        create(
            token,
            tenantId,
            newCreateRequest(
                templateName,
                List.of(
                    Map.of(
                        "key", "portfolioUrl",
                        "label", "Portfolio URL",
                        "type", "TEXT")),
                true));

    if (response == null
        || response.getResult() == ResultType.ERROR
        || response.getData() == null) {
      throw new IllegalStateException("지원서 템플릿 생성 fixture 준비에 실패했습니다.");
    }

    return response.getData().id();
  }

  public ApiResponse<Void> delete(String token, String tenantId, Long templateId) {
    return exchangeWithTenant(
        "/api/v1/workspaces/{workspaceId}/application-templates/{templateId}",
        HttpMethod.DELETE,
        null,
        token,
        tenantId,
        Void.class,
        tenantId,
        templateId);
  }

  public Map<String, Object> newCreateRequest(String name, Object customFields, Boolean used) {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("name", name);
    request.put("customFields", customFields);
    if (used != null) {
      request.put("used", used);
    }
    return request;
  }

  public Map<String, Object> newStatusRequest(Boolean used) {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("used", used);
    return request;
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> exchangeWithTenant(
      String url,
      HttpMethod method,
      Object body,
      String token,
      String tenantId,
      Class<T> responseType,
      Object... urlVariables) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    if (tenantId != null && !tenantId.isBlank()) {
      headers.set("X-Tenant-ID", tenantId);
    }

    HttpEntity<?> entity =
        body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);

    ResponseEntity<ApiResponse> response =
        base.client().exchange(url, method, entity, ApiResponse.class, urlVariables);

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
