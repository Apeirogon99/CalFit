package com.coffiness.calfit.domain.event;

import com.coffiness.calfit.core.enums.ScheduleSyncActionType;
import com.coffiness.calfit.support.event.DomainEvent;

/*
 * 일정 변경 후 Google 반영 요청 정보를 담는 도메인 이벤트
 * */
public record ScheduleGoogleSyncRequestedEvent(
    String tenantId,
    Long userId,
    Long scheduleId,
    ScheduleSyncActionType actionType,
    String googleEventId,
    long occurredAt)
    implements DomainEvent {

  public static ScheduleGoogleSyncRequestedEvent created(
      String tenantId, Long userId, Long scheduleId) {
    return new ScheduleGoogleSyncRequestedEvent(
        tenantId,
        userId,
        scheduleId,
        ScheduleSyncActionType.CREATE,
        null,
        System.currentTimeMillis());
  }

  public static ScheduleGoogleSyncRequestedEvent updated(
      String tenantId, Long userId, Long scheduleId) {
    return new ScheduleGoogleSyncRequestedEvent(
        tenantId,
        userId,
        scheduleId,
        ScheduleSyncActionType.UPDATE,
        null,
        System.currentTimeMillis());
  }

  public static ScheduleGoogleSyncRequestedEvent deleted(
      String tenantId, Long userId, Long scheduleId, String googleEventId) {
    return new ScheduleGoogleSyncRequestedEvent(
        tenantId,
        userId,
        scheduleId,
        ScheduleSyncActionType.DELETE,
        googleEventId,
        System.currentTimeMillis());
  }
}
