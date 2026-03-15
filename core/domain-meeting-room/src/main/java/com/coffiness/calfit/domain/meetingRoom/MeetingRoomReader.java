package com.coffiness.calfit.domain.meetingRoom;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRoomReader {

  boolean existsByName(String name);

  MeetingRoom getMeetingRoom(Long meetingRoomId);

  List<MeetingRoom> getMeetingRooms();

  long countOverlappingReservations(
      Long meetingRoomId, LocalDateTime startDatetime, LocalDateTime endDatetime);

  long countOverlappingReservationsByUser(
      Long userId, LocalDateTime startDatetime, LocalDateTime endDatetime);

  MeetingRoomReservation getActiveReservation(Long meetingRoomId, Long reservationId);

  List<MeetingRoomReservation> getActiveReservations(
      LocalDateTime fromDatetime, LocalDateTime toDatetime);
}
