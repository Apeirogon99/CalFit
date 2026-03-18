package com.coffiness.calfit.domain.event;

import com.coffiness.calfit.core.enums.ScheduleSyncActionType;
import com.coffiness.calfit.domain.*;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 * 일정 변경 이벤트를 받아 구글 캘린더 반영을 수행하는 핸들러
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleGoogleSyncEventHandler {

  private final ExternalCalendarReader externalCalendarReader;
  private final GoogleCalendarTokenService googleCalendarTokenService;
  private final GoogleCalendarPort googleCalendarPort;
  private final ScheduleReader scheduleReader;
  private final ScheduleStore scheduleStore;
  private final ZoneId appZoneId;

  private final ConcurrentMap<Long, ReentrantLock> scheduleLocks = new ConcurrentHashMap<>();

  // 트랜잭션 커밋 이후 비동기로 구글 동기화 이벤트를 처리
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ScheduleGoogleSyncRequestedEvent event) {
    try {
      TenantContext.setTenantId(event.tenantId());
      syncSchedule(event);
    } catch (RuntimeException e) {
      log.error(
          "구글 일정 동기화 실패 - tenantId: {}, scheduleId: {}, action: {}",
          event.tenantId(),
          event.scheduleId(),
          event.actionType(),
          e);
    } finally {
      TenantContext.clear();
    }
  }

  // 액션 타입에 따라 생성/수정/삭제 동기화 분기를 수행
  private void syncSchedule(ScheduleGoogleSyncRequestedEvent event) {
    ExternalCalendar externalCalendar = readSyncEnabledCalendar(event.userId());
    if (externalCalendar == null) {
      return;
    }

    if (!hasText(externalCalendar.calendarId())) {
      log.warn(
          "구글 동기화를 건너뜁니다. externalCalendarId={} 의 calendarId가 비어 있습니다.", externalCalendar.id());
      return;
    }

    String accessToken = googleCalendarTokenService.getValidAccessToken(externalCalendar.id());
    String calendarId = externalCalendar.calendarId();

    if (event.actionType() == ScheduleSyncActionType.CREATE) {
      runWithScheduleLock(
          event.scheduleId(), () -> syncCreate(accessToken, calendarId, event.scheduleId()));
      return;
    }

    if (event.actionType() == ScheduleSyncActionType.UPDATE) {
      runWithScheduleLock(
          event.scheduleId(), () -> syncUpdate(accessToken, calendarId, event.scheduleId()));
      return;
    }

    if (event.actionType() == ScheduleSyncActionType.DELETE) {
      syncDelete(accessToken, calendarId, event.googleEventId());
    }
  }

  // 사용자 기준으로 동기화 활성화된 외부 캘린더를 조회
  private ExternalCalendar readSyncEnabledCalendar(Long userId) {
    return externalCalendarReader.readSyncEnabledByUserId(userId);
  }

  // 일정 생성 이벤트를 구글 일정 생성으로 반영
  private void syncCreate(String accessToken, String calendarId, Long scheduleId) {
    Schedule schedule = scheduleReader.read(scheduleId);
    if (hasText(schedule.googleEventId())) {
      return;
    }

    GoogleCalendarClientResult created =
        googleCalendarPort.createEvent(
            accessToken,
            calendarId,
            schedule.title(),
            schedule.description(),
            toZonedDateTime(schedule.startTime()),
            toZonedDateTime(schedule.endTime()),
            schedule.isAllDay());

    saveGoogleEventIdIfPresent(schedule.id(), created);
  }

  // 일정 수정 이벤트를 구글 일정 수정으로 반영하고 미연결 상태면 생성으로 대체
  private void syncUpdate(String accessToken, String calendarId, Long scheduleId) {
    Schedule schedule = scheduleReader.read(scheduleId);

    if (!hasText(schedule.googleEventId())) {
      GoogleCalendarClientResult created =
          googleCalendarPort.createEvent(
              accessToken,
              calendarId,
              schedule.title(),
              schedule.description(),
              toZonedDateTime(schedule.startTime()),
              toZonedDateTime(schedule.endTime()),
              schedule.isAllDay());

      saveGoogleEventIdIfPresent(schedule.id(), created);
      return;
    }

    GoogleCalendarClientResult updated =
        googleCalendarPort.updateEvent(
            accessToken,
            calendarId,
            schedule.googleEventId(),
            schedule.title(),
            schedule.description(),
            toZonedDateTime(schedule.startTime()),
            toZonedDateTime(schedule.endTime()),
            schedule.isAllDay());

    if (updated != null
        && hasText(updated.googleEventId())
        && !updated.googleEventId().equals(schedule.googleEventId())) {
      scheduleStore.updateGoogleEventId(schedule.id(), updated.googleEventId());
    }
  }

  // 일정 삭제 이벤트를 구글 일정 삭제로 반영
  private void syncDelete(String accessToken, String calendarId, String googleEventId) {
    if (!hasText(googleEventId)) {
      return;
    }

    googleCalendarPort.deleteEvent(accessToken, calendarId, googleEventId);
  }

  // 구글 이벤트 ID가 정상 응답된 경우 내부 일정에 이벤트 ID를 저장
  private void saveGoogleEventIdIfPresent(Long scheduleId, GoogleCalendarClientResult result) {
    if (result == null || !result.status() || !hasText(result.googleEventId())) {
      return;
    }

    scheduleStore.updateGoogleEventId(scheduleId, result.googleEventId());
  }

  // 일정 단위로 동시 동기화 충돌을 방지
  private void runWithScheduleLock(Long scheduleId, Runnable work) {
    if (scheduleId == null) {
      work.run();
      return;
    }

    ReentrantLock lock = scheduleLocks.computeIfAbsent(scheduleId, key -> new ReentrantLock());
    lock.lock();
    try {
      work.run();
    } finally {
      lock.unlock();
      if (!lock.hasQueuedThreads()) {
        scheduleLocks.remove(scheduleId, lock);
      }
    }
  }

  // LocalDateTime을 ZonedDateTime으로 변환
  private ZonedDateTime toZonedDateTime(java.time.LocalDateTime time) {
    return time.atZone(appZoneId);
  }

  // 문자열이 null 또는 공백인지 확인
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
