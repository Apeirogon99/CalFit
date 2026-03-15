package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.AnnouncementBoardCreateRequest;
import com.coffiness.calfit.api.v1.request.AnnouncementBoardUpdateRequest;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardListResponse;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record AnnouncementBoardFixture(BaseFixture base) {

  public static AnnouncementBoardFixture create(
      Environment environment, ObjectMapper objectMapper) {
    return new AnnouncementBoardFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<AnnouncementBoardResponse> create(
      String token, String title, String content, Boolean pinned) {
    return base.post(
        "/api/v1/announcement-boards",
        new AnnouncementBoardCreateRequest(title, content, pinned),
        token,
        AnnouncementBoardResponse.class);
  }

  public ApiResponse<AnnouncementBoardResponse> create(
      String token, String tenantId, String title, String content, Boolean pinned) {
    return exchangeWithTenant(
        "/api/v1/announcement-boards",
        HttpMethod.POST,
        new AnnouncementBoardCreateRequest(title, content, pinned),
        token,
        tenantId,
        AnnouncementBoardResponse.class);
  }

  public ApiResponse<AnnouncementBoardResponse> update(
      String token, long boardId, String title, String content, Boolean pinned) {
    return base.put(
        "/api/v1/announcement-boards/{boardId}",
        new AnnouncementBoardUpdateRequest(title, content, pinned),
        token,
        AnnouncementBoardResponse.class,
        boardId);
  }

  public ApiResponse<AnnouncementBoardResponse> update(
      String token, String tenantId, long boardId, String title, String content, Boolean pinned) {
    return exchangeWithTenant(
        "/api/v1/announcement-boards/" + boardId,
        HttpMethod.PUT,
        new AnnouncementBoardUpdateRequest(title, content, pinned),
        token,
        tenantId,
        AnnouncementBoardResponse.class);
  }

  public ApiResponse<Void> delete(String token, long boardId) {
    return base.delete("/api/v1/announcement-boards/{boardId}", token, Void.class, boardId);
  }

  public ApiResponse<Void> delete(String token, String tenantId, long boardId) {
    return exchangeWithTenant(
        "/api/v1/announcement-boards/" + boardId,
        HttpMethod.DELETE,
        null,
        token,
        tenantId,
        Void.class);
  }

  public ApiResponse<AnnouncementBoardListResponse[]> list(String token) {
    return base.get("/api/v1/announcement-boards", token, AnnouncementBoardListResponse[].class);
  }

  public ApiResponse<AnnouncementBoardListResponse[]> list(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/announcement-boards",
        HttpMethod.GET,
        null,
        token,
        tenantId,
        AnnouncementBoardListResponse[].class);
  }

  public ApiResponse<AnnouncementBoardListResponse[]> list() {
    return base.get("/api/v1/announcement-boards", AnnouncementBoardListResponse[].class);
  }

  public ApiResponse<AnnouncementBoardResponse> detail(
      String token, String tenantId, long boardId) {
    return exchangeWithTenant(
        "/api/v1/announcement-boards/" + boardId,
        HttpMethod.GET,
        null,
        token,
        tenantId,
        AnnouncementBoardResponse.class);
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
        (body != null) ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

    ResponseEntity<ApiResponse> response =
        base.client().exchange(url, method, entity, ApiResponse.class);
    return convertResponse(response.getBody(), responseType);
  }

  @SuppressWarnings("unchecked")
  private <T> ApiResponse<T> convertResponse(ApiResponse<?> response, Class<T> responseType) {
    if (response == null) {
      return null;
    }

    if (response.getResult() == com.coffiness.calfit.core.support.response.ResultType.ERROR
        || response.getData() == null) {
      return (ApiResponse<T>) response;
    }

    if (responseType == Void.class) {
      return (ApiResponse<T>) response;
    }

    T convertedData = base.objectMapper().convertValue(response.getData(), responseType);
    return ApiResponse.success(convertedData);
  }
}
