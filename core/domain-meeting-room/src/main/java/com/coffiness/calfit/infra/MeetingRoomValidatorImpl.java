package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReader;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomValidator;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRoomValidatorImpl implements MeetingRoomValidator {

  private final MeetingRoomReader meetingRoomReader;
  private final MemberReader memberReader;
  private final UserReader userReader;
  private final InterviewScheduleInterviewerRepository interviewScheduleInterviewerRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final MeetingRoomReservationRepository meetingRoomReservationRepository;
  private final ScheduleRepository scheduleRepository;

  @Override
  public void validateCreateInput(
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId) {
    String tenantId = requireTenantId();
    validateHrMember(tenantId, userId);
    if (name == null
        || name.isBlank()
        || location == null
        || capacity == null
        || capacity < 2
        || facilities == null
        || facilities.isEmpty()
        || color == null
        || color.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (description != null && description.length() > 1000) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (meetingRoomReader.existsByName(name)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateUpdateInput(
      Long meetingRoomId,
      String name,
      Integer location,
      Integer capacity,
      String description,
      List<String> facilities,
      String color,
      Long userId) {
    String tenantId = requireTenantId();
    validateHrMember(tenantId, userId);
    if (meetingRoomId == null
        || name == null
        || name.isBlank()
        || location == null
        || capacity == null
        || capacity < 2
        || facilities == null
        || facilities.isEmpty()
        || color == null
        || color.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (description != null && description.length() > 1000) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    MeetingRoom current = meetingRoomReader.getMeetingRoom(meetingRoomId);
    if (!current.name().equals(name) && meetingRoomReader.existsByName(name)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateDeleteRequest(Long meetingRoomId, Long userId) {
    String tenantId = requireTenantId();
    if (userId == null || meetingRoomId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    validateHrMember(tenantId, userId);
    meetingRoomReader.getMeetingRoom(meetingRoomId);
    validateRoomHasNoFutureUsages(tenantId, meetingRoomId);
  }

  @Override
  public void validateReserveRequest(
      Long userId,
      Long meetingRoomId,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime,
      List<Long> participantUserIds) {
    String tenantId = requireTenantId();
    if (userId == null || meetingRoomId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (startDatetime == null || endDatetime == null || !startDatetime.isBefore(endDatetime)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (startDatetime.isBefore(LocalDateTime.now())) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (!userReader.exists(userId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    Member ownerMember = memberReader.getMember(tenantId, userId);
    if (ownerMember == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    meetingRoomReader.getMeetingRoom(meetingRoomId);
    long conflicts =
        meetingRoomReader.countOverlappingReservations(meetingRoomId, startDatetime, endDatetime);
    if (conflicts > 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    validateParticipantScheduleConflicts(
        tenantId, userId, participantUserIds, startDatetime, endDatetime);
  }

  private void validateParticipantScheduleConflicts(
      String tenantId,
      Long ownerUserId,
      List<Long> participantUserIds,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime) {
    LinkedHashSet<Long> uniqueParticipantIds = new LinkedHashSet<>();
    if (ownerUserId != null && ownerUserId > 0) {
      uniqueParticipantIds.add(ownerUserId);
    }
    if (participantUserIds != null) {
      participantUserIds.stream()
          .filter(id -> id != null && id > 0)
          .forEach(uniqueParticipantIds::add);
    }

    List<Long> normalizedParticipantIds = List.copyOf(uniqueParticipantIds);

    if (normalizedParticipantIds.isEmpty()) {
      return;
    }

    List<Member> participants =
        memberReader.getMembersByUserIds(tenantId, normalizedParticipantIds);
    if (participants.size() != normalizedParticipantIds.size()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    boolean invalidTenantMember =
        participants.stream().anyMatch(member -> !tenantId.equals(member.workspaceId()));
    if (invalidTenantMember) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    if (!scheduleRepository
        .findBusyOverlappingSchedulesByUserIds(
            normalizedParticipantIds, startDatetime, endDatetime, EntityStatus.ACTIVE)
        .isEmpty()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    List<Long> interviewScheduleIds =
        interviewScheduleInterviewerRepository
            .findAllByTenantIdAndUserIdIn(tenantId, normalizedParticipantIds)
            .stream()
            .map(mapping -> mapping.getInterviewScheduleId())
            .distinct()
            .toList();
    if (interviewScheduleIds.isEmpty()) {
      return;
    }

    List<InterviewScheduleEntity> schedules =
        interviewScheduleRepository.findAllByTenantIdAndIdInAndStatusNotAndScheduledAtBefore(
            tenantId, interviewScheduleIds, InterviewStatus.CANCELLED, endDatetime);

    for (InterviewScheduleEntity schedule : schedules) {
      LocalDateTime interviewStart = schedule.getScheduledAt();
      LocalDateTime interviewEnd = interviewStart.plusMinutes(schedule.getDurationMinutes());
      if (interviewStart.isBefore(endDatetime) && startDatetime.isBefore(interviewEnd)) {
        throw new CoreException(ErrorType.VALIDATION_ERROR);
      }
    }
  }

  private void validateRoomHasNoFutureUsages(String tenantId, Long meetingRoomId) {
    LocalDateTime now = LocalDateTime.now();

    long reservationCount =
        meetingRoomReservationRepository
            .countByTenantIdAndMeetingRoomIdAndReservationStatusInAndEndDatetimeAfter(
                tenantId,
                meetingRoomId,
                List.of(MeetingRoomStatus.RESERVED, MeetingRoomStatus.ACTIVE),
                now);
    if (reservationCount > 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    long interviewCount =
        interviewScheduleRepository
            .countByTenantIdAndMeetingRoomIdAndInterviewStatusNotAndScheduledAtAfter(
                tenantId, meetingRoomId, InterviewStatus.CANCELLED, now);
    if (interviewCount > 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    long scheduleCount =
        scheduleRepository.countByRoomIdAndStatusAndEndTimeAfter(
            meetingRoomId, EntityStatus.ACTIVE, now);
    if (scheduleCount > 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateCancelReservationRequest(
      Long userId, Long meetingRoomId, Long reservationId) {
    requireTenantId();
    if (userId == null || meetingRoomId == null || reservationId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (!userReader.exists(userId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    MeetingRoomReservation reservation =
        meetingRoomReader.getActiveReservation(meetingRoomId, reservationId);
    if (!userId.equals(reservation.userId()) || reservation.interviewScheduleId() != null) {
      throw new CoreException(ErrorType.FORBIDDEN);
    }
  }

  private Member validateHrMember(String tenantId, Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    Member member = memberReader.getMember(tenantId, userId);
    if (member == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.FORBIDDEN);
    }
    return member;
  }

  private String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }
}
