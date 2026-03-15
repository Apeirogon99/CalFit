package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.ScheduleType;
import java.time.LocalDateTime;
import java.util.List;

/*
 * 선택한 참석자들의 바쁜 일정 현황을 응답하기 위한 모델
 * */
public record ScheduleAvailability(List<AttendeeAvailability> attendeeAvailabilities) {

  public static ScheduleAvailability empty() {
    return new ScheduleAvailability(List.of());
  }

  public record AttendeeAvailability(
      Long attendeeId, String attendeeName, List<BusySchedule> busySchedules) {}

  public record BusySchedule(
      Long scheduleId,
      String title,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime,
      boolean isAllDay,
      ScheduleType type,
      Long interviewScheduleId) {}
}
