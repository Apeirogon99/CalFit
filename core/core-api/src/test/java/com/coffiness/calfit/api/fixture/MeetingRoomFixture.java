package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.MeetingRoomCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomReservationCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomUpdateRequest;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record MeetingRoomFixture(BaseFixture base) {

  public static MeetingRoomFixture create(Environment environment, ObjectMapper objectMapper) {
    return new MeetingRoomFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<MeetingRoomResponse> create(
      String token, String name, Integer location, Integer capacity) {

    return base.post(
        "/api/v1/meeting-rooms",
        new MeetingRoomCreateRequest(name, location, capacity, "", List.of("WiFi"), "#10b981"),
        token,
        MeetingRoomResponse.class);
  }

  public ApiResponse<MeetingRoomResponse> create(
      String token, String tenantId, String name, Integer location, Integer capacity) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms",
        HttpMethod.POST,
        new MeetingRoomCreateRequest(name, location, capacity, "", List.of("WiFi"), "#10b981"),
        token,
        tenantId,
        MeetingRoomResponse.class);
  }

  public ApiResponse<MeetingRoomResponse> create(
      String token,
      String tenantId,
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms",
        HttpMethod.POST,
        new MeetingRoomCreateRequest(name, location, capacity, description, facilities, color),
        token,
        tenantId,
        MeetingRoomResponse.class);
  }

  public ApiResponse<MeetingRoomResponse> update(
      String token, long meetingRoomId, String name, Integer location, Integer capacity) {
    return base.put(
        "/api/v1/meeting-rooms/{meetingRoomId}",
        new MeetingRoomUpdateRequest(name, location, capacity, "", List.of("WiFi"), "#10b981"),
        token,
        MeetingRoomResponse.class,
        meetingRoomId);
  }

  public ApiResponse<MeetingRoomResponse> update(
      String token,
      String tenantId,
      long meetingRoomId,
      String name,
      Integer location,
      Integer capacity) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms/" + meetingRoomId,
        HttpMethod.PUT,
        new MeetingRoomUpdateRequest(name, location, capacity, "", List.of("WiFi"), "#10b981"),
        token,
        tenantId,
        MeetingRoomResponse.class);
  }

  public ApiResponse<MeetingRoomResponse> update(
      String token,
      String tenantId,
      long meetingRoomId,
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms/" + meetingRoomId,
        HttpMethod.PUT,
        new MeetingRoomUpdateRequest(name, location, capacity, description, facilities, color),
        token,
        tenantId,
        MeetingRoomResponse.class);
  }

  public ApiResponse<Void> delete(String token, long meetingRoomId) {
    return base.delete("/api/v1/meeting-rooms/{meetingRoomId}", token, Void.class, meetingRoomId);
  }

  public ApiResponse<Void> delete(String token, String tenantId, long meetingRoomId) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms/" + meetingRoomId,
        HttpMethod.DELETE,
        null,
        token,
        tenantId,
        Void.class);
  }

  public ApiResponse<MeetingRoomReservationResponse> reserve(
      String token,
      String tenantId,
      long meetingRoomId,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime) {
    return reserve(
        token, tenantId, meetingRoomId, "테스트 예약", null, startDatetime, endDatetime, List.of());
  }

  public ApiResponse<MeetingRoomReservationResponse> reserve(
      String token,
      String tenantId,
      long meetingRoomId,
      String title,
      String description,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime,
      List<Long> participantUserIds) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms/" + meetingRoomId + "/reservations",
        HttpMethod.POST,
        new MeetingRoomReservationCreateRequest(
            title, description, startDatetime, endDatetime, participantUserIds),
        token,
        tenantId,
        MeetingRoomReservationResponse.class);
  }

  public ApiResponse<MeetingRoomReservationResponse> cancelReservation(
      String token, String tenantId, long meetingRoomId, long reservationId) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms/" + meetingRoomId + "/reservations/" + reservationId,
        HttpMethod.DELETE,
        null,
        token,
        tenantId,
        MeetingRoomReservationResponse.class);
  }

  public ApiResponse<MeetingRoomReservationResponse[]> listReservations(
      String token, String tenantId, LocalDateTime fromDatetime, LocalDateTime toDatetime) {
    String url =
        "/api/v1/meeting-rooms/reservations?fromDatetime="
            + fromDatetime
            + "&toDatetime="
            + toDatetime;
    return exchangeWithTenant(
        url, HttpMethod.GET, null, token, tenantId, MeetingRoomReservationResponse[].class);
  }

  public ApiResponse<MeetingRoomResponse[]> list(String token) {
    return base.get("/api/v1/meeting-rooms", token, MeetingRoomResponse[].class);
  }

  public ApiResponse<MeetingRoomResponse[]> list(String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/meeting-rooms",
        HttpMethod.GET,
        null,
        token,
        tenantId,
        MeetingRoomResponse[].class);
  }

  public ApiResponse<MeetingRoomResponse[]> list() {
    return base.get("/api/v1/meeting-rooms", MeetingRoomResponse[].class);
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
