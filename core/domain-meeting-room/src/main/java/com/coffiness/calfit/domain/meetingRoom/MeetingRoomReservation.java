package com.coffiness.calfit.domain.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import java.time.LocalDateTime;

public record MeetingRoomReservation(
    Long id,
    Long meetingRoomId,
    Long userId,
    Long interviewScheduleId,
    LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    MeetingRoomStatus status) {}
