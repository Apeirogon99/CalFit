package com.coffiness.calfit.domain.event;

import com.coffiness.calfit.core.enums.ScheduleSyncActionType;
import com.coffiness.calfit.support.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;

/*
 * 일정 생성, 수정, 삭제 시 참석자 알림 발송에 사용하는 이벤트
 */
public record ScheduleAttendeeNotificationEvent(
    String tenantId,
    Long scheduleId,
    Long ownerUserId,
    String title,
    LocalDateTime startTime,
    LocalDateTime endTime,
    boolean isAllDay,
    List<Long> attendeeUserIds,
    ScheduleSyncActionType actionType,
    long occurredAt)
    implements DomainEvent {

  // 일정 생성 알림 이벤트 생성
  public static ScheduleAttendeeNotificationEvent created(
      String tenantId,
      Long scheduleId,
      Long ownerUserId,
      String title,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isAllDay,
      List<Long> attendeeUserIds) {
    return new ScheduleAttendeeNotificationEvent(
        tenantId,
        scheduleId,
        ownerUserId,
        title,
        startTime,
        endTime,
        isAllDay,
        attendeeUserIds,
        ScheduleSyncActionType.CREATE,
        System.currentTimeMillis());
  }

  // 일정 수정 알림 이벤트 생성
  public static ScheduleAttendeeNotificationEvent updated(
      String tenantId,
      Long scheduleId,
      Long ownerUserId,
      String title,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isAllDay,
      List<Long> attendeeUserIds) {
    return new ScheduleAttendeeNotificationEvent(
        tenantId,
        scheduleId,
        ownerUserId,
        title,
        startTime,
        endTime,
        isAllDay,
        attendeeUserIds,
        ScheduleSyncActionType.UPDATE,
        System.currentTimeMillis());
  }

  // 일정 삭제 알림 이벤트 생성
  public static ScheduleAttendeeNotificationEvent deleted(
      String tenantId,
      Long scheduleId,
      Long ownerUserId,
      String title,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isAllDay,
      List<Long> attendeeUserIds) {
    return new ScheduleAttendeeNotificationEvent(
        tenantId,
        scheduleId,
        ownerUserId,
        title,
        startTime,
        endTime,
        isAllDay,
        attendeeUserIds,
        ScheduleSyncActionType.DELETE,
        System.currentTimeMillis());
  }
}
