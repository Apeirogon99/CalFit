package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingRoomReservationCreateRequest(
    String title,
    String description,
    @NotNull LocalDateTime startDatetime,
    @NotNull LocalDateTime endDatetime,
    List<Long> participantUserIds) {}
