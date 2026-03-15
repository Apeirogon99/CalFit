package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.ScheduleType;
import java.time.LocalDateTime;
import java.util.List;

public record ScheduleDetailInfo(
    Long id,
    String title,
    LocalDateTime startTime,
    LocalDateTime endTime,
    ScheduleType type,
    String description,
    Long roomId,
    Long reservationId,
    boolean isAllDay,
    boolean isBusy,
    Long interviewScheduleId,
    String location,
    String ownerName,
    List<Long> attendeeIds,
    List<String> attendees,
    String applicantName) {

  public static ScheduleDetailInfo of(
      Schedule schedule,
      String location,
      String ownerName,
      List<Long> attendeeIds,
      List<String> attendees,
      String applicantName) {
    return new ScheduleDetailInfo(
        schedule.id(),
        schedule.title(),
        schedule.startTime(),
        schedule.endTime(),
        schedule.type(),
        schedule.description(),
        schedule.roomId(),
        schedule.reservationId(),
        schedule.isAllDay(),
        schedule.isBusy(),
        schedule.interviewScheduleId(),
        location,
        ownerName,
        attendeeIds,
        attendees,
        applicantName);
  }
}
