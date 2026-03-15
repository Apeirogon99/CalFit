package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingRoomReservationResponse(
    Long id,
    Long meetingRoomId,
    Long userId,
    Long interviewScheduleId,
    String title,
    String description,
    String organizerName,
    List<String> attendees,
    LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    MeetingRoomStatus status) {

  public static MeetingRoomReservationResponse from(MeetingRoomReservation reservation) {
    return fromFallback(reservation);
  }

  public static MeetingRoomReservationResponse fromFallback(MeetingRoomReservation reservation) {
    String fallbackTitle = reservation.interviewScheduleId() != null ? "면접 일정" : "회의실 예약";
    return new MeetingRoomReservationResponse(
        reservation.id(),
        reservation.meetingRoomId(),
        reservation.userId(),
        reservation.interviewScheduleId(),
        fallbackTitle,
        null,
        null,
        List.of(),
        reservation.startDatetime(),
        reservation.endDatetime(),
        reservation.status());
  }
}
