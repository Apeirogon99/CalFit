package com.coffiness.calfit.domain.meetingRoom;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRoomValidator {

  void validateCreateInput(
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId);

  void validateUpdateInput(
      Long meetingRoomId,
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId);

  void validateDeleteRequest(Long meetingRoomId, Long userId);

  void validateReserveRequest(
      Long userId,
      Long meetingRoomId,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime,
      List<Long> participantUserIds);

  void validateCancelReservationRequest(Long userId, Long meetingRoomId, Long reservationId);
}
