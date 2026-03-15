package com.coffiness.calfit.core.api.facade.calendar;

import com.coffiness.calfit.domain.ScheduleAvailability;
import com.coffiness.calfit.domain.ScheduleDetailInfo;
import com.coffiness.calfit.domain.ScheduleInfo;
import com.coffiness.calfit.domain.ScheduleService;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReader;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationCreatedEvent;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationService;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * 일정 API의 멤버 검증과 회의실 예약 연동 담당
 * */
@Component
@RequiredArgsConstructor
public class ScheduleFacade {

  private final MemberReader memberReader;
  private final ScheduleService scheduleService;
  private final MeetingRoomReservationService meetingRoomReservationService;
  private final MeetingRoomReader meetingRoomReader;
  private final DomainEventPublisher domainEventPublisher;

  private Member validateAndGetMember(long userId) {
    String currentWorkspaceId = currentWorkspaceId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);
    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }
    return member;
  }

  @Transactional
  public void createSchedule(long userId, ScheduleCreateRequest request) {
    validateAndGetMember(userId);
    List<Long> attendeeIds = validateAndNormalizeAttendeeIds(userId, request.attendeeIds());
    ScheduleCreateRequest normalizedRequest =
        new ScheduleCreateRequest(
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay(),
            request.roomId(),
            request.isBusy(),
            attendeeIds);
    Long reservationId = null;

    if (normalizedRequest.roomId() != null) {
      MeetingRoomReservation reservation =
          meetingRoomReservationService.reserve(
              userId,
              normalizedRequest.roomId(),
              normalizedRequest.startTime(),
              normalizedRequest.endTime(),
              List.of());
      reservationId = reservation.id();
      publishMeetingRoomReservationCreated(
          userId,
          reservation.id(),
          normalizedRequest.roomId(),
          normalizedRequest.title(),
          normalizedRequest.startTime(),
          normalizedRequest.endTime(),
          attendeeIds);
    }

    scheduleService.createSchedule(userId, reservationId, normalizedRequest);
  }

  @Transactional
  public void syncSchedule(long userId, ScheduleSyncRequest request) {
    validateAndGetMember(userId);

    scheduleService.upsertScheduleByGoogleEventId(userId, request);
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {
    validateAndGetMember(userId);

    return scheduleService.getSchedules(userId, startDate, endDate);
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long userId, Long scheduleId) {
    validateAndGetMember(userId);

    return scheduleService.getDetailSchedule(userId, scheduleId);
  }

  // 선택한 참석자들의 일정 현황을 조회
  @Transactional(readOnly = true)
  public ScheduleAvailability getAttendeeAvailability(
      long userId, LocalDateTime startDate, LocalDateTime endDate, List<Long> attendeeIds) {
    validateAndGetMember(userId);

    if (attendeeIds == null || attendeeIds.isEmpty()) {
      return ScheduleAvailability.empty();
    }

    List<Long> normalizedAttendeeIds = validateAndNormalizeAttendeeIds(userId, attendeeIds, true);
    return scheduleService.getAttendeeAvailability(startDate, endDate, normalizedAttendeeIds);
  }

  @Transactional
  public ScheduleDetailInfo updateSchedule(
      long userId, Long scheduleId, ScheduleUpdateRequest request) {
    validateAndGetMember(userId);
    List<Long> attendeeIds =
        request.attendeeIds() != null
            ? validateAndNormalizeAttendeeIds(userId, request.attendeeIds(), false)
            : null;
    ScheduleUpdateRequest normalizedRequest =
        new ScheduleUpdateRequest(
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay(),
            request.roomId(),
            request.isBusy(),
            attendeeIds);
    ScheduleDetailInfo scheduleDetailInfo = scheduleService.getDetailSchedule(userId, scheduleId);

    Long targetRoomId =
        normalizedRequest.roomId() != null
            ? normalizedRequest.roomId()
            : scheduleDetailInfo.roomId();
    Long newReservationId = scheduleDetailInfo.reservationId();

    boolean roomIdChanged =
        normalizedRequest.roomId() != null
            && !normalizedRequest.roomId().equals(scheduleDetailInfo.roomId());
    boolean timeChanged =
        (normalizedRequest.startTime() != null
                && !normalizedRequest.startTime().equals(scheduleDetailInfo.startTime()))
            || (normalizedRequest.endTime() != null
                && !normalizedRequest.endTime().equals(scheduleDetailInfo.endTime()));

    if (scheduleDetailInfo.roomId() != null && scheduleDetailInfo.reservationId() != null) {
      if (roomIdChanged || timeChanged) {
        meetingRoomReservationService.cancelReservation(
            userId, scheduleDetailInfo.roomId(), scheduleDetailInfo.reservationId());
        newReservationId = null;
      }
    }

    // 회의실 또는 시간 정보가 변경되면 새 예약을 생성한다.
    if (targetRoomId != null && (roomIdChanged || timeChanged)) {
      LocalDateTime startTime =
          normalizedRequest.startTime() != null
              ? normalizedRequest.startTime()
              : scheduleDetailInfo.startTime();
      LocalDateTime endTime =
          normalizedRequest.endTime() != null
              ? normalizedRequest.endTime()
              : scheduleDetailInfo.endTime();

      MeetingRoomReservation reservation =
          meetingRoomReservationService.reserve(
              userId, targetRoomId, startTime, endTime, List.of());
      newReservationId = reservation.id();
      publishMeetingRoomReservationCreated(
          userId,
          reservation.id(),
          targetRoomId,
          normalizedRequest.title() != null
              ? normalizedRequest.title()
              : scheduleDetailInfo.title(),
          startTime,
          endTime,
          attendeeIds != null ? attendeeIds : scheduleDetailInfo.attendeeIds());
    }

    return scheduleService.updateSchedule(userId, scheduleId, newReservationId, normalizedRequest);
  }

  @Transactional
  public void deleteSchedule(long userId, Long scheduleId) {
    validateAndGetMember(userId);

    ScheduleDetailInfo scheduleInfo = scheduleService.getDetailSchedule(userId, scheduleId);

    if (scheduleInfo.roomId() != null && scheduleInfo.reservationId() != null) {
      meetingRoomReservationService.cancelReservation(
          userId, scheduleInfo.roomId(), scheduleInfo.reservationId());
    }

    scheduleService.deleteSchedule(userId, scheduleId);
  }

  private List<Long> validateAndNormalizeAttendeeIds(long ownerUserId, List<Long> attendeeIds) {
    return validateAndNormalizeAttendeeIds(ownerUserId, attendeeIds, false);
  }

  private List<Long> validateAndNormalizeAttendeeIds(
      long ownerUserId, List<Long> attendeeIds, boolean includeOwnerUserId) {
    if (attendeeIds == null) {
      return null;
    }

    LinkedHashSet<Long> uniqueAttendeeIds = new LinkedHashSet<>();
    for (Long attendeeId : attendeeIds) {
      if (attendeeId == null || attendeeId <= 0) {
        throw new IllegalArgumentException("유효하지 않은 참석자 ID가 포함되어 있습니다.");
      }
      if (!includeOwnerUserId && attendeeId.equals(ownerUserId)) {
        continue;
      }
      uniqueAttendeeIds.add(attendeeId);
    }

    if (uniqueAttendeeIds.isEmpty()) {
      return List.of();
    }

    List<Long> normalizedAttendeeIds = new ArrayList<>(uniqueAttendeeIds);
    List<Member> members =
        memberReader.getMembersByUserIds(currentWorkspaceId(), normalizedAttendeeIds);

    if (members.size() != normalizedAttendeeIds.size()) {
      throw new IllegalArgumentException("워크스페이스에 속하지 않은 참석자가 포함되어 있습니다.");
    }

    return normalizedAttendeeIds;
  }

  private String currentWorkspaceId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("워크스페이스 ID가 필요합니다.");
    }
    return tenantId;
  }

  private void publishMeetingRoomReservationCreated(
      long userId,
      Long reservationId,
      Long roomId,
      String title,
      LocalDateTime startTime,
      LocalDateTime endTime,
      List<Long> attendeeIds) {
    if (reservationId == null || roomId == null || startTime == null || endTime == null) {
      return;
    }

    domainEventPublisher.publish(
        MeetingRoomReservationCreatedEvent.of(
            currentWorkspaceId(),
            reservationId,
            userId,
            roomId,
            meetingRoomReader.getMeetingRoom(roomId).name(),
            title,
            startTime,
            endTime,
            attendeeIds));
  }
}
