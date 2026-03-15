package com.coffiness.calfit.core.api.controller.v1.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentInterviewersUpdateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentUpdateRequest;
import com.coffiness.calfit.api.v1.response.InterviewScheduleResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.api.v1.response.WeeklyInterviewScheduleResponse;
import com.coffiness.calfit.core.api.facade.recruitment.RecruitmentFacade;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import com.coffiness.calfit.domain.interview.WeeklyInterviewScheduleItem;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentListInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
public class RecruitmentController {

  private final RecruitmentService recruitmentService;
  private final RecruitmentFacade recruitmentFacade;

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/api/v1/recruitments")
  public ApiResponse<Long> createRecruitment(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody RecruitmentCreateRequest request) {

    long userId = user.userId();
    Long recruitmentId = recruitmentFacade.createRecruitment(userId, request);

    return ApiResponse.success(recruitmentId);
  }

  @GetMapping("/api/v1/recruitments")
  public ApiResponse<List<RecruitmentListResponse>> getRecruitmentList(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) RecruitmentStatus recruitmentStatus,
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    long userId = user.userId();

    List<RecruitmentListInfo> infos =
        recruitmentService.getRecruitmentList(userId, recruitmentStatus, pageable);

    List<RecruitmentListResponse> response =
        infos.stream().map(RecruitmentListResponse::from).toList();

    return ApiResponse.success(response);
  }

  @GetMapping("/api/v1/recruitments/{recruitmentId}")
  public ApiResponse<RecruitmentDetailResponse> getRecruitmentDetail(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long recruitmentId) {
    long userId = user.userId();

    RecruitmentDetailInfo info = recruitmentService.getRecruitmentDetail(userId, recruitmentId);

    return ApiResponse.success(RecruitmentDetailResponse.from(info));
  }

  @GetMapping("/api/v1/recruitments/{recruitmentId}/interview-schedules")
  public ApiResponse<List<InterviewScheduleResponse>> getInterviewSchedule(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long recruitmentId,
      @RequestParam String yearMonth) {
    long userId = user.userId();

    List<InterviewScheduleCalendarItem> items =
        recruitmentFacade.getInterviewSchedule(userId, recruitmentId, yearMonth);

    List<InterviewScheduleResponse> response =
        items.stream().map(InterviewScheduleResponse::from).toList();

    return ApiResponse.success(response);
  }

  // 메인 채용 화면에서 쓸 이번 주 면접 일정을 내려줌
  @GetMapping("/api/v1/recruitments/weekly-interview-schedules")
  public ApiResponse<List<WeeklyInterviewScheduleResponse>> getWeeklyInterviewSchedules(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    long userId = user.userId();

    List<WeeklyInterviewScheduleItem> items =
        recruitmentFacade.getWeeklyInterviewSchedules(userId, date);

    List<WeeklyInterviewScheduleResponse> response =
        items.stream().map(WeeklyInterviewScheduleResponse::from).toList();

    return ApiResponse.success(response);
  }

  @PutMapping("/api/v1/recruitments/{recruitmentId}")
  public ApiResponse<RecruitmentDetailResponse> updateRecruitment(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long recruitmentId,
      @Valid @RequestBody RecruitmentUpdateRequest request) {
    long userId = user.userId();
    RecruitmentDetailInfo info =
        recruitmentFacade.updateRecruitment(userId, recruitmentId, request);

    return ApiResponse.success(RecruitmentDetailResponse.from(info));
  }

  // 게시 이후에도 면접관만 따로 수정할 수 있는 API
  @PatchMapping("/api/v1/recruitments/{recruitmentId}/interviewers")
  public ApiResponse<RecruitmentDetailResponse> updateRecruitmentInterviewers(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long recruitmentId,
      @Valid @RequestBody RecruitmentInterviewersUpdateRequest request) {
    long userId = user.userId();
    RecruitmentDetailInfo info =
        recruitmentFacade.updateRecruitmentInterviewers(userId, recruitmentId, request);

    return ApiResponse.success(RecruitmentDetailResponse.from(info));
  }

  // DRAFT 공고를 즉시 게시하는 API
  @PatchMapping("/api/v1/recruitments/{recruitmentId}/publish")
  public ApiResponse<RecruitmentDetailResponse> publishRecruitment(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long recruitmentId) {
    long userId = user.userId();
    RecruitmentDetailInfo info = recruitmentFacade.publishRecruitment(userId, recruitmentId);

    return ApiResponse.success(RecruitmentDetailResponse.from(info));
  }

  @DeleteMapping("/api/v1/recruitments/{recruitmentId}")
  public ApiResponse<Long> deleteRecruitment(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long recruitmentId) {
    long userId = user.userId();
    recruitmentFacade.deleteRecruitment(userId, recruitmentId);

    return ApiResponse.success(recruitmentId);
  }
}
