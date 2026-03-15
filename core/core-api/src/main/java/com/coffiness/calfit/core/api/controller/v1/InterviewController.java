package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.InterviewCreateRequest;
import com.coffiness.calfit.api.v1.response.InterviewAvailabilityResponse;
import com.coffiness.calfit.api.v1.response.InterviewPendingStageResponse;
import com.coffiness.calfit.api.v1.response.InterviewResponse;
import com.coffiness.calfit.core.api.facade.interview.InterviewFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.interview.Interview;
import com.coffiness.calfit.domain.interview.InterviewAvailability;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interviews")
public class InterviewController {

  private final InterviewFacade interviewFacade;

  // 회의실/면접관의 점유 시간(겹치는 슬롯 전체)을 조회
  @GetMapping("/availability")
  public ApiResponse<InterviewAvailabilityResponse> availability(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam(required = false) List<Long> meetingRoomIds,
      @RequestParam(required = false) List<Long> interviewerIds,
      @RequestParam(required = false) List<Long> applicantIds) {

    Long userId = user.userId();

    InterviewAvailability availability =
        interviewFacade.getAvailability(userId, from, meetingRoomIds, interviewerIds, applicantIds);

    return ApiResponse.success(InterviewAvailabilityResponse.from(availability));
  }

  // 채용 공고의 면접 단계별 대기 지원자 목록을 조회
  @GetMapping("/pending-applicants")
  public ApiResponse<List<InterviewPendingStageResponse>> pendingApplicants(
      @AuthenticationPrincipal SecurityUser user, @RequestParam Long recruitmentId) {

    Long userId = user.userId();

    return ApiResponse.success(
        interviewFacade.getPendingInterviewStages(userId, recruitmentId).stream()
            .map(InterviewPendingStageResponse::from)
            .toList());
  }

  // 일정 생성 즉시 확정(CONFIRMED)
  @PostMapping
  public ApiResponse<InterviewResponse> create(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody InterviewCreateRequest request) {

    Long userId = user.userId();

    Interview interview = interviewFacade.createInterview(userId, request);
    return ApiResponse.success(InterviewResponse.from(interview));
  }
}
