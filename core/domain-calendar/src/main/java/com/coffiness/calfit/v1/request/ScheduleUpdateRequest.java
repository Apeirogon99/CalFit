package com.coffiness.calfit.v1.request;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record ScheduleUpdateRequest(
    @NotBlank(message = "일정 제목은 필수입니다.") String title,
    String description,
    @NotNull(message = "일정 타입은 필수입니다.") ScheduleType type,
    @NotNull(message = "시작 시간은 필수입니다.") LocalDateTime startTime,
    @NotNull(message = "종료 시간은 필수입니다.") LocalDateTime endTime,
    @JsonProperty("isAllDay") Boolean isAllDay,
    Long roomId,
    @JsonProperty("isBusy") Boolean isBusy,
    List<Long> attendeeIds) {}
