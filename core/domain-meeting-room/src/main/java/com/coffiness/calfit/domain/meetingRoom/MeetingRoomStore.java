package com.coffiness.calfit.domain.meetingRoom;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRoomStore {

  MeetingRoom create(
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId);

  MeetingRoom update(
      Long meetingRoomId,
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId);

  void delete(Long meetingRoomId, Long userId);

  MeetingRoomReservation reserve(
      Long meetingRoomId, Long userId, LocalDateTime startDatetime, LocalDateTime endDatetime);

  MeetingRoomReservation cancelReservation(Long meetingRoomId, Long reservationId, Long userId);

  int syncReservationStatuses();
}
