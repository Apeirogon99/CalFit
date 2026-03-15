package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.InterviewCreateRequest;
import com.coffiness.calfit.api.v1.response.InterviewAvailabilityResponse;
import com.coffiness.calfit.api.v1.response.InterviewPendingStageResponse;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.core.enums.InterviewRound;
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

public record InterviewFixture(BaseFixture base) {

  public static InterviewFixture create(Environment environment, ObjectMapper objectMapper) {
    return new InterviewFixture(BaseFixture.create(environment, objectMapper));
  }

  public ApiResponse<InterviewAvailabilityResponse> availability(
      String token,
      String tenantId,
      LocalDateTime from,
      List<Long> meetingRoomIds,
      List<Long> interviewerIds) {
    String url = buildAvailabilityUrl(from, meetingRoomIds, interviewerIds);
    return exchangeWithTenant(
        url, HttpMethod.GET, null, token, tenantId, InterviewAvailabilityResponse.class);
  }

  public ApiResponse<InterviewAvailabilityResponse> availabilityWithoutFrom(
      String token, String tenantId) {
    return exchangeWithTenant(
        "/api/v1/interviews/availability",
        HttpMethod.GET,
        null,
        token,
        tenantId,
        InterviewAvailabilityResponse.class);
  }

  // 채용 공고 기준 면접 단계별 대기 지원자 목록을 조회
  public ApiResponse<List<InterviewPendingStageResponse>> getPendingApplicants(
      String token, String tenantId, Long recruitmentId) {
    String url =
        String.format("/api/v1/interviews/pending-applicants?recruitmentId=%s", recruitmentId);
    ApiResponse<InterviewPendingStageResponse[]> response =
        exchangeWithTenant(
            url, HttpMethod.GET, null, token, tenantId, InterviewPendingStageResponse[].class);

    if (response == null
        || response.getResult() == com.coffiness.calfit.core.support.response.ResultType.ERROR
        || response.getData() == null) {
      @SuppressWarnings("unchecked")
      ApiResponse<List<InterviewPendingStageResponse>> casted =
          (ApiResponse<List<InterviewPendingStageResponse>>) (ApiResponse<?>) response;
      return casted;
    }

    return ApiResponse.success(List.of(response.getData()));
  }

  public ApiResponse<InterviewResponse> create(
      String token,
      String tenantId,
      Long recruitmentId,
      Long recruitmentStageId,
      InterviewRound round,
      List<Long> interviewerIds,
      List<Long> applicantIds,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo) {
    return exchangeWithTenant(
        "/api/v1/interviews",
        HttpMethod.POST,
        new InterviewCreateRequest(
            recruitmentId,
            recruitmentStageId,
            round,
            interviewerIds,
            applicantIds,
            meetingRoomId,
            scheduledAt,
            durationMinutes,
            memo),
        token,
        tenantId,
        InterviewResponse.class);
  }

  private String buildAvailabilityUrl(
      LocalDateTime from, List<Long> meetingRoomIds, List<Long> interviewerIds) {
    StringBuilder url = new StringBuilder("/api/v1/interviews/availability");
    if (from != null) {
      url.append("?from=").append(from);
    }

    appendListQuery(url, "meetingRoomIds", meetingRoomIds);
    appendListQuery(url, "interviewerIds", interviewerIds);
    return url.toString();
  }

  private void appendListQuery(StringBuilder url, String key, List<Long> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    for (Long value : values) {
      if (value == null) {
        continue;
      }
      if (url.indexOf("?") < 0) {
        url.append('?');
      } else {
        url.append('&');
      }
      url.append(key).append('=').append(value);
    }
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
