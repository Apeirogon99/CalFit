package com.coffiness.calfit.core.api.facade.meetingRoom;

import com.coffiness.calfit.api.v1.request.MeetingRoomReservationCreateRequest;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.domain.Schedule;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleReader;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.domain.interview.InterviewService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReader;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationCreatedEvent;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationService;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRoomReservationFacade {

  private final MeetingRoomReservationService meetingRoomReservationService;
  private final InterviewService interviewService;
  private final ScheduleService scheduleService;
  private final ScheduleReader scheduleReader;
  private final MeetingRoomReader meetingRoomReader;
  private final DomainEventPublisher domainEventPublisher;

  @Transactional
  public MeetingRoomReservation reserveMeetingRoom(
      Long userId, Long meetingRoomId, MeetingRoomReservationCreateRequest request) {
    MeetingRoomReservation reservation =
        meetingRoomReservationService.reserve(
            userId,
            meetingRoomId,
            request.startDatetime(),
            request.endDatetime(),
            request.participantUserIds());

    scheduleService.createMeetingRoomReservationSchedule(
        userId,
        reservation.id(),
        meetingRoomId,
        request.title(),
        request.description(),
        request.startDatetime(),
        request.endDatetime(),
        request.participantUserIds());

    MeetingRoom meetingRoom = meetingRoomReader.getMeetingRoom(meetingRoomId);
    domainEventPublisher.publish(
        MeetingRoomReservationCreatedEvent.of(
            currentTenantId(),
            reservation.id(),
            userId,
            meetingRoomId,
            meetingRoom.name(),
            request.title(),
            request.startDatetime(),
            request.endDatetime(),
            request.participantUserIds()));

    return reservation;
  }

  @Transactional(readOnly = true)
  public List<MeetingRoomReservation> getActiveReservations(
      LocalDateTime fromDatetime, LocalDateTime toDatetime) {
    return meetingRoomReservationService.listActiveReservations(fromDatetime, toDatetime);
  }

  @Transactional(readOnly = true)
  public List<MeetingRoomReservationResponse> getActiveReservationResponses(
      LocalDateTime fromDatetime, LocalDateTime toDatetime) {
    return meetingRoomReservationService.listActiveReservations(fromDatetime, toDatetime).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public MeetingRoomReservationResponse toResponse(MeetingRoomReservation reservation) {
    List<Schedule> schedules;
    try {
      schedules = scheduleReader.findByReservationId(reservation.id());
    } catch (RuntimeException exception) {
      log.warn(
          "Failed to load schedules for meeting room reservation. reservationId={}, meetingRoomId={}, interviewScheduleId={}",
          reservation.id(),
          reservation.meetingRoomId(),
          reservation.interviewScheduleId(),
          exception);
      return MeetingRoomReservationResponse.fromFallback(reservation);
    }

    if (schedules.isEmpty()) {
      return MeetingRoomReservationResponse.fromFallback(reservation);
    }

    Schedule representativeSchedule = selectRepresentativeSchedule(reservation, schedules);
    try {
      ScheduleDetailInfo scheduleDetail = scheduleReader.readDetail(representativeSchedule.id());
      return new MeetingRoomReservationResponse(
          reservation.id(),
          reservation.meetingRoomId(),
          reservation.userId(),
          reservation.interviewScheduleId(),
          scheduleDetail.title(),
          scheduleDetail.description(),
          scheduleDetail.ownerName(),
          scheduleDetail.attendees(),
          reservation.startDatetime(),
          reservation.endDatetime(),
          reservation.status());
    } catch (RuntimeException exception) {
      log.warn(
          "Failed to build meeting room reservation response. reservationId={}, meetingRoomId={}, interviewScheduleId={}, scheduleId={}",
          reservation.id(),
          reservation.meetingRoomId(),
          reservation.interviewScheduleId(),
          representativeSchedule.id(),
          exception);
      return MeetingRoomReservationResponse.fromFallback(reservation);
    }
  }

  @Transactional
  public MeetingRoomReservation cancelReservation(
      Long userId, Long meetingRoomId, Long reservationId) {
    MeetingRoomReservation canceled =
        meetingRoomReservationService.cancelReservation(userId, meetingRoomId, reservationId);

    if (canceled.interviewScheduleId() != null) {
      interviewService.cancelConfirmedSchedule(userId, canceled.interviewScheduleId());
    }

    scheduleService.deleteSchedulesByReservationId(canceled.id());

    return canceled;
  }

  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    return tenantId;
  }

  private Schedule selectRepresentativeSchedule(
      MeetingRoomReservation reservation, List<Schedule> schedules) {
    return schedules.stream()
        .filter(schedule -> schedule.roomId() != null)
        .filter(schedule -> schedule.roomId().equals(reservation.meetingRoomId()))
        .findFirst()
        .orElse(schedules.get(0));
  }
}
