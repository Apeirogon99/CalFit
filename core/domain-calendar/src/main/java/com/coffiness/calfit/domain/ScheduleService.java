package com.coffiness.calfit.domain;

import com.coffiness.calfit.domain.event.ScheduleAttendeeNotificationEvent;
import com.coffiness.calfit.domain.event.ScheduleGoogleSyncRequestedEvent;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleSyncRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

  // TODO : 추후 권한 관련 보안 예외 처리 도입

  private final ScheduleReader scheduleReader;
  private final ScheduleStore scheduleStore;
  private final DomainEventPublisher domainEventPublisher;

  public void createSchedule(Long userId, Long reservationId, ScheduleCreateRequest request) {
    Schedule newSchedule =
        new Schedule(
            null,
            userId,
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay() != null ? request.isAllDay() : false,
            request.roomId(),
            reservationId,
            request.isBusy() != null ? request.isBusy() : true,
            null,
            null);

    validateBusyScheduleConflicts(
        userId,
        request.attendeeIds(),
        newSchedule.startTime(),
        newSchedule.endTime(),
        newSchedule.isBusy(),
        null);

    Schedule saved = scheduleStore.store(newSchedule, request.attendeeIds());
    publishScheduleAttendeeNotification(saved, request.attendeeIds(), true);

    domainEventPublisher.publish(
        ScheduleGoogleSyncRequestedEvent.created(currentTenantId(), userId, saved.id()));
  }

  public void createMeetingRoomReservationSchedule(
      Long userId,
      Long reservationId,
      Long roomId,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      List<Long> attendeeIds) {
    Schedule schedule =
        new Schedule(
            null,
            userId,
            title == null || title.isBlank() ? "회의실 예약" : title,
            description,
            com.coffiness.calfit.core.enums.ScheduleType.MEETING,
            startTime,
            endTime,
            false,
            roomId,
            reservationId,
            true,
            null,
            null);

    validateBusyScheduleConflicts(
        userId, attendeeIds, schedule.startTime(), schedule.endTime(), schedule.isBusy(), null);

    Schedule saved = scheduleStore.store(schedule, attendeeIds);
    publishScheduleAttendeeNotification(saved, attendeeIds, true);

    domainEventPublisher.publish(
        ScheduleGoogleSyncRequestedEvent.created(currentTenantId(), userId, saved.id()));
  }

  @Transactional(readOnly = true)
  public List<ScheduleInfo> getSchedules(
      long userId, LocalDateTime startDate, LocalDateTime endDate) {

    List<Schedule> schedules = scheduleReader.findOverlappingSchedules(userId, startDate, endDate);
    List<Long> roomIds =
        schedules.stream()
            .map(Schedule::roomId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, String> locationsByRoomId = scheduleReader.readLocationsByRoomIds(roomIds);

    return schedules.stream()
        .map(
            schedule ->
                ScheduleInfo.of(
                    schedule,
                    schedule.roomId() == null
                        ? ""
                        : locationsByRoomId.getOrDefault(schedule.roomId(), "알 수 없는 장소")))
        .toList();
  }

  // 선택한 참석자들의 일정 현황을 조회
  @Transactional(readOnly = true)
  public ScheduleAvailability getAttendeeAvailability(
      LocalDateTime startDate, LocalDateTime endDate, List<Long> attendeeIds) {
    if (attendeeIds == null || attendeeIds.isEmpty()) {
      return ScheduleAvailability.empty();
    }

    return scheduleReader.readAttendeeAvailability(attendeeIds, startDate, endDate);
  }

  @Transactional(readOnly = true)
  public ScheduleDetailInfo getDetailSchedule(long userId, Long scheduleId) {

    Schedule schedule = scheduleReader.read(scheduleId);
    List<Long> attendeeIds = scheduleReader.readAttendeeIds(scheduleId);

    boolean isOwner = schedule.userId().equals(userId);
    boolean isAttendee = attendeeIds.contains(userId);

    if (!isOwner && !isAttendee) {
      throw new IllegalArgumentException("해당 일정을 조회할 권한이 없습니다.");
    }

    return scheduleReader.readDetail(scheduleId);
  }

  public ScheduleDetailInfo updateSchedule(
      long userId, Long scheduleId, Long reservationId, @Valid ScheduleUpdateRequest request) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 일정을 수정할 권한이 없습니다.");
    }
    if (schedule.interviewScheduleId() != null) {
      throw new IllegalArgumentException("면접 일정으로 생성된 일정은 수정할 수 없습니다.");
    }

    List<Long> previousAttendeeIds = scheduleReader.readAttendeeIds(scheduleId);

    Schedule updatedSchedule =
        new Schedule(
            schedule.id(),
            schedule.userId(),
            request.title() != null ? request.title() : schedule.title(),
            request.description() != null ? request.description() : schedule.description(),
            request.type() != null ? request.type() : schedule.type(),
            request.startTime() != null ? request.startTime() : schedule.startTime(),
            request.endTime() != null ? request.endTime() : schedule.endTime(),
            request.isAllDay() != null ? request.isAllDay() : schedule.isAllDay(),
            request.roomId() != null ? request.roomId() : schedule.roomId(),
            reservationId,
            request.isBusy() != null ? request.isBusy() : schedule.isBusy(),
            schedule.googleEventId(),
            schedule.interviewScheduleId());

    List<Long> updatedAttendeeIds =
        request.attendeeIds() != null ? request.attendeeIds() : previousAttendeeIds;

    validateBusyScheduleConflicts(
        schedule.userId(),
        updatedAttendeeIds,
        updatedSchedule.startTime(),
        updatedSchedule.endTime(),
        updatedSchedule.isBusy(),
        scheduleId);

    scheduleStore.update(updatedSchedule, updatedAttendeeIds);
    publishScheduleUpdateNotifications(updatedSchedule, previousAttendeeIds, updatedAttendeeIds);

    domainEventPublisher.publish(
        ScheduleGoogleSyncRequestedEvent.updated(currentTenantId(), userId, scheduleId));

    return getDetailSchedule(userId, scheduleId);
  }

  public void deleteSchedule(long userId, Long scheduleId) {

    Schedule schedule = scheduleReader.read(scheduleId);

    if (!schedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 일정을 삭제할 권한이 없습니다.");
    }
    if (schedule.interviewScheduleId() != null) {
      throw new IllegalArgumentException("면접 일정으로 생성된 일정은 삭제할 수 없습니다.");
    }

    List<Long> attendeeIds = scheduleReader.readAttendeeIds(scheduleId);
    String googleEventId = schedule.googleEventId();

    // 참석자도 삭제
    // TODO : 일정 밑에 있는 참석자는 Hard vs Soft? Soft면 Repository에 'DELETED' 검증이 필요할까?
    scheduleStore.delete(schedule);
    publishScheduleAttendeeNotification(schedule, attendeeIds, false);

    domainEventPublisher.publish(
        ScheduleGoogleSyncRequestedEvent.deleted(
            currentTenantId(), userId, scheduleId, googleEventId));
  }

  public void deleteSchedulesByReservationId(Long reservationId) {
    List<Schedule> schedules = scheduleReader.findByReservationId(reservationId);
    // 예약에 연결된 일정 참석자 일괄 조회
    Map<Long, List<Long>> attendeeIdsByScheduleId =
        scheduleReader.readAttendeeIdsByScheduleIds(schedules.stream().map(Schedule::id).toList());

    for (Schedule schedule : schedules) {
      List<Long> attendeeIds = attendeeIdsByScheduleId.getOrDefault(schedule.id(), List.of());
      String googleEventId = schedule.googleEventId();
      scheduleStore.delete(schedule);
      publishScheduleAttendeeNotification(schedule, attendeeIds, false);
      domainEventPublisher.publish(
          ScheduleGoogleSyncRequestedEvent.deleted(
              currentTenantId(), schedule.userId(), schedule.id(), googleEventId));
    }
  }

  public void deleteScheduleByGoogleEventId(long userId, String googleEventId) {
    if (googleEventId == null || googleEventId.isBlank()) {
      throw new IllegalArgumentException("구글 이벤트 ID는 필수입니다.");
    }

    Schedule existingSchedule = scheduleReader.readByGoogleEventId(googleEventId);

    if (existingSchedule == null) {
      return;
    }

    if (!existingSchedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 구글 일정을 삭제할 권한이 없습니다.");
    }

    scheduleStore.delete(existingSchedule);
  }

  public Long upsertScheduleByGoogleEventId(long userId, ScheduleSyncRequest request) {
    if (request.googleEventId() == null || request.googleEventId().isBlank()) {
      throw new IllegalArgumentException("구글 이벤트 ID는 필수입니다.");
    }

    Schedule existingSchedule = scheduleReader.readByGoogleEventId(request.googleEventId());

    if (existingSchedule == null) {
      Schedule newSchedule =
          new Schedule(
              null,
              userId,
              request.title(),
              request.description(),
              request.type(),
              request.startTime(),
              request.endTime(),
              request.isAllDay(),
              null,
              null,
              true,
              request.googleEventId(),
              null);

      Schedule saved = scheduleStore.store(newSchedule, List.of());
      return saved.id();
    }

    if (!existingSchedule.userId().equals(userId)) {
      throw new IllegalArgumentException("해당 구글 일정을 수정할 권한이 없습니다.");
    }

    Schedule updatedSchedule =
        new Schedule(
            existingSchedule.id(),
            existingSchedule.userId(),
            request.title(),
            request.description(),
            request.type(),
            request.startTime(),
            request.endTime(),
            request.isAllDay(),
            existingSchedule.roomId(),
            existingSchedule.reservationId(),
            existingSchedule.isBusy(),
            existingSchedule.googleEventId(),
            existingSchedule.interviewScheduleId());

    List<Long> attendeeIds = scheduleReader.readAttendeeIds(existingSchedule.id());
    scheduleStore.update(updatedSchedule, attendeeIds);

    return updatedSchedule.id();
  }

  // 생성자와 참석자의 바쁜 일정 충돌 여부를 검사
  private void validateBusyScheduleConflicts(
      Long ownerUserId,
      List<Long> attendeeIds,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isBusy,
      Long excludedScheduleId) {
    if (!isBusy || startTime == null || endTime == null) {
      return;
    }

    LinkedHashSet<Long> targetUserIds = new LinkedHashSet<>();
    if (ownerUserId != null && ownerUserId > 0) {
      targetUserIds.add(ownerUserId);
    }
    if (attendeeIds != null) {
      attendeeIds.stream()
          .filter(attendeeId -> attendeeId != null && attendeeId > 0)
          .forEach(targetUserIds::add);
    }

    if (targetUserIds.isEmpty()) {
      return;
    }

    List<Long> normalizedUserIds = List.copyOf(targetUserIds);
    boolean hasConflict =
        excludedScheduleId == null
            ? scheduleReader.existsBusyOverlappingScheduleConflict(
                normalizedUserIds, startTime, endTime)
            : scheduleReader.existsBusyOverlappingScheduleConflictExcludingSchedule(
                excludedScheduleId, normalizedUserIds, startTime, endTime);

    if (hasConflict) {
      throw new IllegalArgumentException("참석자 또는 생성자에게 같은 시간대의 바쁜 일정이 있습니다. 시간을 변경해 주세요.");
    }
  }

  // 참석자 수정 알림 발행
  private void publishScheduleUpdateNotifications(
      Schedule schedule, List<Long> previousAttendeeIds, List<Long> updatedAttendeeIds) {
    List<Long> normalizedPreviousAttendeeIds =
        normalizeAttendeeIds(schedule.userId(), previousAttendeeIds);
    List<Long> normalizedUpdatedAttendeeIds =
        normalizeAttendeeIds(schedule.userId(), updatedAttendeeIds);

    List<Long> retainedAttendeeIds =
        normalizedUpdatedAttendeeIds.stream()
            .filter(normalizedPreviousAttendeeIds::contains)
            .toList();
    List<Long> addedAttendeeIds =
        normalizedUpdatedAttendeeIds.stream()
            .filter(attendeeId -> !normalizedPreviousAttendeeIds.contains(attendeeId))
            .toList();
    List<Long> removedAttendeeIds =
        normalizedPreviousAttendeeIds.stream()
            .filter(attendeeId -> !normalizedUpdatedAttendeeIds.contains(attendeeId))
            .toList();

    if (!retainedAttendeeIds.isEmpty()) {
      domainEventPublisher.publish(
          ScheduleAttendeeNotificationEvent.updated(
              currentTenantId(),
              schedule.id(),
              schedule.userId(),
              schedule.title(),
              schedule.startTime(),
              schedule.endTime(),
              schedule.isAllDay(),
              retainedAttendeeIds));
    }

    if (!addedAttendeeIds.isEmpty()) {
      domainEventPublisher.publish(
          ScheduleAttendeeNotificationEvent.created(
              currentTenantId(),
              schedule.id(),
              schedule.userId(),
              schedule.title(),
              schedule.startTime(),
              schedule.endTime(),
              schedule.isAllDay(),
              addedAttendeeIds));
    }

    if (!removedAttendeeIds.isEmpty()) {
      domainEventPublisher.publish(
          ScheduleAttendeeNotificationEvent.deleted(
              currentTenantId(),
              schedule.id(),
              schedule.userId(),
              schedule.title(),
              schedule.startTime(),
              schedule.endTime(),
              schedule.isAllDay(),
              removedAttendeeIds));
    }
  }

  // 참석자 생성/삭제 알림 발행
  private void publishScheduleAttendeeNotification(
      Schedule schedule, List<Long> attendeeIds, boolean created) {
    List<Long> normalizedAttendeeIds = normalizeAttendeeIds(schedule.userId(), attendeeIds);
    if (normalizedAttendeeIds.isEmpty()) {
      return;
    }

    domainEventPublisher.publish(
        created
            ? ScheduleAttendeeNotificationEvent.created(
                currentTenantId(),
                schedule.id(),
                schedule.userId(),
                schedule.title(),
                schedule.startTime(),
                schedule.endTime(),
                schedule.isAllDay(),
                normalizedAttendeeIds)
            : ScheduleAttendeeNotificationEvent.deleted(
                currentTenantId(),
                schedule.id(),
                schedule.userId(),
                schedule.title(),
                schedule.startTime(),
                schedule.endTime(),
                schedule.isAllDay(),
                normalizedAttendeeIds));
  }

  // 참석자 목록 정규화
  private List<Long> normalizeAttendeeIds(Long ownerUserId, List<Long> attendeeIds) {
    if (attendeeIds == null || attendeeIds.isEmpty()) {
      return List.of();
    }

    LinkedHashSet<Long> normalizedAttendeeIds = new LinkedHashSet<>();
    attendeeIds.stream()
        .filter(attendeeId -> attendeeId != null && attendeeId > 0)
        .filter(attendeeId -> !attendeeId.equals(ownerUserId))
        .forEach(normalizedAttendeeIds::add);

    return List.copyOf(normalizedAttendeeIds);
  }

  // 현재 테넌트 ID 조회
  private String currentTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalStateException("TENANT_ID_REQUIRED");
    }
    return tenantId;
  }
}
