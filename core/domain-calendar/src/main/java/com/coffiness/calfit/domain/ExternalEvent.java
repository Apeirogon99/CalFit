package com.coffiness.calfit.domain;

import com.coffiness.calfit.core.enums.EventStatus;
import java.time.LocalDateTime;

public record ExternalEvent(
    Long id,
    Long externalCalendarId,
    String googleEventId,
    String title,
    String description,
    LocalDateTime startTime,
    LocalDateTime endTime,
    boolean isAllDay,
    EventStatus status) {

  public ExternalEvent {
    validateTime(startTime, endTime);
  }

  private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
    if (startTime == null || endTime == null) {
      throw new IllegalArgumentException("일정의 시작 시간과 종료 시간은 필수입니다.");
    }
    if (startTime.isAfter(endTime)) {
      throw new IllegalArgumentException("일정 시작 시간이 종료 시간보다 늦을 수 없습니다.");
    }
  }

  // 유효한 시간인지
  private boolean isOccupyingTime() {
    return this.status != EventStatus.CANCELLED;
  }
}
