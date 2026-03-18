package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.api.v1.response.InterviewScheduleResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.api.v1.response.WeeklyInterviewScheduleResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

public record RecruitmentFixture(BaseFixture base) {

  public static RecruitmentFixture create(Environment environment, ObjectMapper objectMapper) {
    return new RecruitmentFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<Void> createRecruitment(String token, RecruitmentCreateRequest request) {
    return base.post("/api/v1/recruitments", request, token, Void.class);
  }

  public ApiResponse<List<RecruitmentListResponse>> getRecruitmentList(String token) {
    ApiResponse<RecruitmentListResponse[]> response =
        base.get("/api/v1/recruitments", token, RecruitmentListResponse[].class);

    return convertArrayResponse(response);
  }

  public ApiResponse<RecruitmentDetailResponse> getRecruitmentDetail(
      String token, Long recruitmentId) {
    String url = String.format("/api/v1/recruitments/%s", recruitmentId);
    return base.get(url, token, RecruitmentDetailResponse.class);
  }

  public ApiResponse<RecruitmentDetailResponse> updateRecruitment(
      String token, Long recruitmentId, RecruitmentUpdateRequest request) {
    String url = String.format("/api/v1/recruitments/%s", recruitmentId);
    return base.put(url, request, token, RecruitmentDetailResponse.class);
  }

  public ApiResponse<Void> deleteRecruitment(String token, Long recruitmentId) {
    String url = String.format("/api/v1/recruitments/%s", recruitmentId);
    return base.delete(url, token, Void.class);
  }

  // ==================== tenantId 지원 메서드  ====================

  public ApiResponse<Void> createRecruitment(
      String token, String tenantId, RecruitmentCreateRequest request) {
    return exchangeWithTenant(
        "/api/v1/recruitments", HttpMethod.POST, request, token, tenantId, Void.class);
  }

  public ApiResponse<List<RecruitmentListResponse>> getRecruitmentList(
      String token, String tenantId) {
    ApiResponse<RecruitmentListResponse[]> response =
        exchangeWithTenant(
            "/api/v1/recruitments",
            HttpMethod.GET,
            null,
            token,
            tenantId,
            RecruitmentListResponse[].class);

    return convertArrayResponse(response);
  }

  public ApiResponse<RecruitmentDetailResponse> getRecruitmentDetail(
      String token, String tenantId, Long recruitmentId) {
    String url = String.format("/api/v1/recruitments/%s", recruitmentId);
    return exchangeWithTenant(
        url, HttpMethod.GET, null, token, tenantId, RecruitmentDetailResponse.class);
  }

  public ApiResponse<List<InterviewScheduleResponse>> getRecruitmentSchedule(
      String token, String tenantId, Long recruitmentId, String yearMonth) {

    String url =
        String.format(
            "/api/v1/recruitments/%s/interview-schedules?yearMonth=%s", recruitmentId, yearMonth);
    ApiResponse<InterviewScheduleResponse[]> response =
        exchangeWithTenant(
            url, HttpMethod.GET, null, token, tenantId, InterviewScheduleResponse[].class);

    return convertArrayResponse(response);
  }

  // 이번 주 면접 일정 조회 API를 tenant 헤더와 함께 호출
  public ApiResponse<List<WeeklyInterviewScheduleResponse>> getWeeklyInterviewSchedules(
      String token, String tenantId, LocalDate date) {
    String url = String.format("/api/v1/recruitments/weekly-interview-schedules?date=%s", date);
    ApiResponse<WeeklyInterviewScheduleResponse[]> response =
        exchangeWithTenant(
            url, HttpMethod.GET, null, token, tenantId, WeeklyInterviewScheduleResponse[].class);

    return convertArrayResponse(response);
  }

  public ApiResponse<RecruitmentDetailResponse> updateRecruitment(
      String token, String tenantId, Long recruitmentId, RecruitmentUpdateRequest request) {
    String url = String.format("/api/v1/recruitments/%s", recruitmentId);
    return exchangeWithTenant(
        url, HttpMethod.PUT, request, token, tenantId, RecruitmentDetailResponse.class);
  }

  public ApiResponse<RecruitmentDetailResponse> updateRecruitmentInterviewers(
      String token, String tenantId, Long recruitmentId, Object request) {
    String url = String.format("/api/v1/recruitments/%s/interviewers", recruitmentId);
    return exchangeWithTenant(
        url, HttpMethod.PATCH, request, token, tenantId, RecruitmentDetailResponse.class);
  }

  public ApiResponse<RecruitmentDetailResponse> publishRecruitment(
      String token, String tenantId, Long recruitmentId) {
    String url = String.format("/api/v1/recruitments/%s/publish", recruitmentId);
    return exchangeWithTenant(
        url, HttpMethod.PATCH, null, token, tenantId, RecruitmentDetailResponse.class);
  }

  public ApiResponse<Void> deleteRecruitment(String token, String tenantId, Long recruitmentId) {
    String url = String.format("/api/v1/recruitments/%s", recruitmentId);
    return exchangeWithTenant(url, HttpMethod.DELETE, null, token, tenantId, Void.class);
  }

  // ==================== 토큰 헬퍼 메소드 ====================

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
