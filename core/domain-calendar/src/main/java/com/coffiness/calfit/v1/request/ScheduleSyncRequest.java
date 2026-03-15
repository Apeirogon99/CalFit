package com.coffiness.calfit.v1.request;

import com.coffiness.calfit.core.enums.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/*
 * 구글 캘린더 이벤트를 Calfit 일정으로 동기화할 때 사용하는 DTO
 * */
public record ScheduleSyncRequest(
    @NotBlank(message = "구글 이벤트 ID는 필수입니다.") String googleEventId,
    @NotBlank(message = "일정 제목은 필수입니다.") String title,
    String description,
    @NotNull(message = "일정 타입은 필수입니다.") ScheduleType type,
    @NotNull(message = "시작 시간은 필수입니다.") LocalDateTime startTime,
    @NotNull(message = "종료 시간은 필수입니다.") LocalDateTime endTime,
    boolean isAllDay) {

  public ScheduleSyncRequest {
    if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
      throw new IllegalArgumentException("종료 시간은 시작 시간보다 이후여야 합니다.");
    }
  }
}
