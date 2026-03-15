package com.coffiness.calfit.domain.meetingRoom;

import com.coffiness.calfit.support.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingRoomReservationCreatedEvent(
    String tenantId,
    Long reservationId,
    Long actorUserId,
    Long meetingRoomId,
    String meetingRoomName,
    String title,
    LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    List<Long> participantUserIds,
    long occurredAt)
    implements DomainEvent {

  public static MeetingRoomReservationCreatedEvent of(
      String tenantId,
      Long reservationId,
      Long actorUserId,
      Long meetingRoomId,
      String meetingRoomName,
      String title,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime,
      List<Long> participantUserIds) {
    return new MeetingRoomReservationCreatedEvent(
        tenantId,
        reservationId,
        actorUserId,
        meetingRoomId,
        meetingRoomName,
        title,
        startDatetime,
        endDatetime,
        participantUserIds == null ? List.of() : List.copyOf(participantUserIds),
        System.currentTimeMillis());
  }
}
