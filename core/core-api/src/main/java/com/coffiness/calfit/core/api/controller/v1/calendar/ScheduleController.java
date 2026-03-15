package com.coffiness.calfit.core.api.controller.v1.calendar;

import com.coffiness.calfit.core.api.facade.calendar.ScheduleFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.ScheduleAvailability;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleInfo;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import com.coffiness.calfit.v1.response.ScheduleAvailabilityResponse;
import com.coffiness.calfit.v1.response.ScheduleDetailResponse;
import com.coffiness.calfit.v1.response.ScheduleResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

  private final ScheduleFacade scheduleFacade;

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/api/v1/schedules")
  public ApiResponse<Void> createSchedule(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody ScheduleCreateRequest request) {

    scheduleFacade.createSchedule(user.userId(), request);

    return ApiResponse.success(null);
  }

  // 일정 동기화
  @PostMapping("/api/v1/schedules/sync")
  public ApiResponse<Void> syncSchedule(
      @AuthenticationPrincipal SecurityUser user, @Valid @RequestBody ScheduleSyncRequest request) {

    scheduleFacade.syncSchedule(user.userId(), request);

    return ApiResponse.success(null);
  }

  @GetMapping("/api/v1/schedules")
  public ApiResponse<List<ScheduleResponse>> getSchedules(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

    // 날짜를 하루의 시작/끝 시각으로 변환
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusNanos(1);

    List<ScheduleInfo> infos =
        scheduleFacade.getSchedules(user.userId(), startDateTime, endDateTime);

    List<ScheduleResponse> response = infos.stream().map(ScheduleResponse::from).toList();

    return ApiResponse.success(response);
  }

  // 선택한 참석자들의 당일 바쁜 일정 현황을 조회
  @GetMapping("/api/v1/schedules/availability")
  public ApiResponse<ScheduleAvailabilityResponse> getAttendeeAvailability(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
      @RequestParam(required = false) List<Long> attendeeIds) {

    LocalDateTime startDateTime = date.atStartOfDay();
    LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();

    ScheduleAvailability availability =
        scheduleFacade.getAttendeeAvailability(
            user.userId(), startDateTime, endDateTime, attendeeIds);

    return ApiResponse.success(ScheduleAvailabilityResponse.from(availability));
  }

  // 선택한 참석자들의 기간별 바쁜 일정 현황 조회
  @GetMapping("/api/v1/schedules/availability/range")
  public ApiResponse<ScheduleAvailabilityResponse> getAttendeeAvailabilityRange(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
      @RequestParam(required = false) List<Long> attendeeIds) {

    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

    ScheduleAvailability availability =
        scheduleFacade.getAttendeeAvailability(
            user.userId(), startDateTime, endDateTime, attendeeIds);

    return ApiResponse.success(ScheduleAvailabilityResponse.from(availability));
  }

  @GetMapping("/api/v1/schedules/{scheduleId}")
  public ApiResponse<ScheduleDetailResponse> getDetailSchedule(
      @AuthenticationPrincipal SecurityUser user, @PathVariable("scheduleId") Long scheduleId) {

    ScheduleDetailInfo info = scheduleFacade.getDetailSchedule(user.userId(), scheduleId);
    ScheduleDetailResponse response = ScheduleDetailResponse.from(info);

    return ApiResponse.success(response);
  }

  @PutMapping("/api/v1/schedules/{scheduleId}")
  public ApiResponse<ScheduleDetailResponse> updateSchedule(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable("scheduleId") Long scheduleId,
      @Valid @RequestBody ScheduleUpdateRequest request) {

    ScheduleDetailInfo info = scheduleFacade.updateSchedule(user.userId(), scheduleId, request);

    return ApiResponse.success(ScheduleDetailResponse.from(info));
  }

  @DeleteMapping("/api/v1/schedules/{scheduleId}")
  public ApiResponse<Void> deleteSchedule(
      @AuthenticationPrincipal SecurityUser user, @PathVariable("scheduleId") Long scheduleId) {

    scheduleFacade.deleteSchedule(user.userId(), scheduleId);
    return ApiResponse.success(null);
  }
}
