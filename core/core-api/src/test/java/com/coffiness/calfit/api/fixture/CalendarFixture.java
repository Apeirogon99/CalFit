package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.CalendarConnectRequest;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import com.coffiness.calfit.v1.response.ScheduleAvailabilityResponse;
import com.coffiness.calfit.v1.response.ScheduleDetailResponse;
import com.coffiness.calfit.v1.response.ScheduleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record CalendarFixture(BaseFixture base) {

  public static CalendarFixture create(Environment environment, ObjectMapper objectMapper) {
    return new CalendarFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<String> connectGoogleCalendar(String token, String tenantId, String authCode) {
    CalendarConnectRequest request =
        new CalendarConnectRequest(authCode, "http://localhost:5173/auth/callback");

    return exchangeWithTenant(
        "/api/v1/calendars/google/connect",
        HttpMethod.POST,
        request,
        token,
        tenantId,
        String.class);
  }

  public ApiResponse<Void> createSchedule(
      String token, String tenantId, ScheduleCreateRequest request) {
    return exchangeWithTenant(
        "/api/v1/schedules", HttpMethod.POST, request, token, tenantId, Void.class);
  }

  public ApiResponse<Void> syncSchedule(
      String token, String tenantId, ScheduleSyncRequest request) {
    return exchangeWithTenant(
        "/api/v1/schedules/sync", HttpMethod.POST, request, token, tenantId, Void.class);
  }

  public ApiResponse<Void> syncGoogleCalendar(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/calendars/google/sync", HttpMethod.POST, null, token, tenantId, Void.class);
  }

  @SuppressWarnings("unchecked")
  public ApiResponse<List<ScheduleResponse>> getSchedules(
      String token, String tenantId, String startDate, String endDate) {
    String url = String.format("/api/v1/schedules?startDate=%s&endDate=%s", startDate, endDate);

    ApiResponse<ScheduleResponse[]> response =
        exchangeWithTenant(url, HttpMethod.GET, null, token, tenantId, ScheduleResponse[].class);

    if (response == null
        || response.getResult() == ResultType.ERROR
        || response.getData() == null) {
      return (ApiResponse<List<ScheduleResponse>>) (ApiResponse<?>) response;
    }

    return ApiResponse.success(List.of(response.getData()));
  }

  public ApiResponse<ScheduleDetailResponse> getDetailSchedule(
      String token, String tenantId, Long scheduleId) {
    String url = String.format("/api/v1/schedules/%d", scheduleId);
    return exchangeWithTenant(
        url, HttpMethod.GET, null, token, tenantId, ScheduleDetailResponse.class);
  }

  public ApiResponse<ScheduleAvailabilityResponse> getScheduleAvailability(
      String token, String tenantId, String date, List<Long> attendeeIds) {
    String attendeeQuery =
        attendeeIds == null || attendeeIds.isEmpty()
            ? ""
            : "&"
                + attendeeIds.stream()
                    .map(attendeeId -> "attendeeIds=" + attendeeId)
                    .reduce((left, right) -> left + "&" + right)
                    .orElse("");
    String url = String.format("/api/v1/schedules/availability?date=%s%s", date, attendeeQuery);
    return exchangeWithTenant(
        url, HttpMethod.GET, null, token, tenantId, ScheduleAvailabilityResponse.class);
  }

  public ApiResponse<Void> updateSchedule(
      String token, String tenantId, Long scheduleId, ScheduleUpdateRequest request) {
    String url = String.format("/api/v1/schedules/%d", scheduleId);

    return exchangeWithTenant(url, HttpMethod.PUT, request, token, tenantId, Void.class);
  }

  public ApiResponse<Void> deleteSchedule(String token, String tenantId, Long scheduleId) {
    String url = String.format("/api/v1/schedules/%d", scheduleId);

    return exchangeWithTenant(url, HttpMethod.DELETE, null, token, tenantId, Void.class);
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
    if (response == null) return null;
    if (response.getResult() == ResultType.ERROR || response.getData() == null) {
      return (ApiResponse<T>) response;
    }
    if (responseType == Void.class) return (ApiResponse<T>) response;
    T converted = base.objectMapper().convertValue(response.getData(), responseType);
    return ApiResponse.success(converted);
  }
}
