package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.api.v1.response.NotificationUnreadCountResponse;
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

public record NotificationFixture(BaseFixture base) {

  public static NotificationFixture create(Environment environment, ObjectMapper objectMapper) {
    return new NotificationFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<NotificationUnreadCountResponse> unreadCount(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/notifications/unread-count",
        HttpMethod.GET,
        null,
        token,
        tenantId,
        NotificationUnreadCountResponse.class);
  }

  public ApiResponse<NotificationPageResponse> list(
      String token, String tenantId, String type, int page, int size) {
    return exchangeWithTenant(
        buildListUrl("/api/v1/notifications", type, page, size),
        HttpMethod.GET,
        null,
        token,
        tenantId,
        NotificationPageResponse.class);
  }

  public ApiResponse<NotificationPageResponse> unread(
      String token, String tenantId, String type, int page, int size) {
    return exchangeWithTenant(
        buildListUrl("/api/v1/notifications/unread", type, page, size),
        HttpMethod.GET,
        null,
        token,
        tenantId,
        NotificationPageResponse.class);
  }

  public ApiResponse<NotificationResponse> read(
      String token, String tenantId, Long notificationId) {
    return exchangeWithTenant(
        "/api/v1/notifications/" + notificationId + "/read",
        HttpMethod.PATCH,
        null,
        token,
        tenantId,
        NotificationResponse.class);
  }

  public ApiResponse<Void> readAll(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/notifications/read-all", HttpMethod.PATCH, null, token, tenantId, Void.class);
  }

  public ApiResponse<Void> delete(String token, String tenantId, Long notificationId) {
    return exchangeWithTenant(
        "/api/v1/notifications/" + notificationId,
        HttpMethod.DELETE,
        null,
        token,
        tenantId,
        Void.class);
  }

  public ApiResponse<Void> deleteAll(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/notifications", HttpMethod.DELETE, null, token, tenantId, Void.class);
  }

  private String buildListUrl(String baseUrl, String type, int page, int size) {
    StringBuilder url = new StringBuilder(baseUrl);
    url.append("?page=").append(page).append("&size=").append(size);
    if (type != null && !type.isBlank()) {
      url.append("&type=").append(type);
    }
    return url.toString();
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
    T convertedData = base.objectMapper().convertValue(response.getData(), responseType);
    return ApiResponse.success(convertedData);
  }

  public record NotificationPageResponse(List<NotificationResponse> contents, Boolean hasNext) {}
}
