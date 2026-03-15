package com.coffiness.calfit.v1.response;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.domain.ScheduleAvailability;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/*
 * 참석자 일정 현황 조회 API 응답 DTO
 * */
public record ScheduleAvailabilityResponse(
    List<AttendeeAvailabilityResponse> attendeeAvailabilities) {

  public static ScheduleAvailabilityResponse from(ScheduleAvailability availability) {
    return new ScheduleAvailabilityResponse(
        availability.attendeeAvailabilities().stream()
            .map(AttendeeAvailabilityResponse::from)
            .toList());
  }

  public record AttendeeAvailabilityResponse(
      Long attendeeId, String attendeeName, List<BusyScheduleResponse> busySchedules) {

    public static AttendeeAvailabilityResponse from(
        ScheduleAvailability.AttendeeAvailability availability) {
      return new AttendeeAvailabilityResponse(
          availability.attendeeId(),
          availability.attendeeName(),
          availability.busySchedules().stream().map(BusyScheduleResponse::from).toList());
    }
  }

  public record BusyScheduleResponse(
      Long scheduleId,
      String title,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime,
      @JsonProperty("isAllDay") boolean isAllDay,
      ScheduleType type,
      Long interviewScheduleId) {

    public static BusyScheduleResponse from(ScheduleAvailability.BusySchedule busySchedule) {
      return new BusyScheduleResponse(
          busySchedule.scheduleId(),
          busySchedule.title(),
          busySchedule.startDateTime(),
          busySchedule.endDateTime(),
          busySchedule.isAllDay(),
          busySchedule.type(),
          busySchedule.interviewScheduleId());
    }
  }
}
