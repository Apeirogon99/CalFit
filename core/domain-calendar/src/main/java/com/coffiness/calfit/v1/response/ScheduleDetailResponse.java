package com.coffiness.calfit.v1.response;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ScheduleDetailResponse(
    Long id,
    String title,
    String date,
    String startTime,
    String endTime,
    ScheduleType type,
    String description,
    Long roomId,
    @JsonProperty("isAllDay") boolean isAllDay,
    @JsonProperty("isBusy") boolean isBusy,
    Long interviewScheduleId,
    String location,
    String ownerName,
    List<Long> attendeeIds,
    List<String> attendees,
    String applicantName) {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public static ScheduleDetailResponse from(ScheduleDetailInfo info) {
    return new ScheduleDetailResponse(
        info.id(),
        info.title(),
        info.startTime().format(DATE_FORMATTER),
        info.startTime().format(TIME_FORMATTER),
        info.endTime().format(TIME_FORMATTER),
        info.type(),
        info.description(),
        info.roomId(),
        info.isAllDay(),
        info.isBusy(),
        info.interviewScheduleId(),
        info.location(),
        info.ownerName(),
        info.attendeeIds(),
        info.attendees(),
        info.applicantName());
  }
}
