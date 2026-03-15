package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.ScheduleType;
import java.time.LocalDateTime;

public record ScheduleInfo(
    Long id,
    String title,
    LocalDateTime startTime,
    LocalDateTime endTime,
    ScheduleType type,
    String description,
    String location,
    Long roomId,
    Long reservationId,
    boolean isAllDay,
    boolean isBusy,
    Long interviewScheduleId) {

  public static ScheduleInfo of(Schedule schedule, String location) {
    return new ScheduleInfo(
        schedule.id(),
        schedule.title(),
        schedule.startTime(),
        schedule.endTime(),
        schedule.type(),
        schedule.description(),
        location,
        schedule.roomId(),
        schedule.reservationId(),
        schedule.isAllDay(),
        schedule.isBusy(),
        schedule.interviewScheduleId());
  }
}
