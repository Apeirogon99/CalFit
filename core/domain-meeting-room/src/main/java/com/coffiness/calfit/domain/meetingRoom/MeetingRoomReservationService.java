package com.coffiness.calfit.domain.meetingRoom;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingRoomReservationService {

  private final MeetingRoomReader meetingRoomReader;
  private final MeetingRoomStore meetingRoomStore;
  private final MeetingRoomValidator meetingRoomValidator;

  @Transactional(readOnly = true)
  public List<MeetingRoomReservation> listActiveReservations(
      LocalDateTime fromDatetime, LocalDateTime toDatetime) {
    return meetingRoomReader.getActiveReservations(fromDatetime, toDatetime);
  }

  @Transactional
  public MeetingRoomReservation reserve(
      Long userId,
      Long meetingRoomId,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime,
      List<Long> participantUserIds) {
    meetingRoomValidator.validateReserveRequest(
        userId, meetingRoomId, startDatetime, endDatetime, participantUserIds);
    return meetingRoomStore.reserve(meetingRoomId, userId, startDatetime, endDatetime);
  }

  @Transactional
  public MeetingRoomReservation cancelReservation(
      Long userId, Long meetingRoomId, Long reservationId) {
    meetingRoomValidator.validateCancelReservationRequest(userId, meetingRoomId, reservationId);
    return meetingRoomStore.cancelReservation(meetingRoomId, reservationId, userId);
  }

  @Transactional
  public int updateReservationStatusesBySchedule() {
    return meetingRoomStore.syncReservationStatuses();
  }
}
