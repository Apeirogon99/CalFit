package com.coffiness.calfit.api;

import com.coffiness.calfit.dto.*;
import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult.SyncEventModel;
import com.coffiness.calfit.model.GoogleCalendarWatchResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import feign.FeignException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleCalendarClient implements GoogleCalendarPort {

  private static final String CALENDAR_NAME_PREFIX = "CalFit - ";
  private static final String DEFAULT_WORKSPACE_NAME = "Workspace";
  private static final String WORKSPACE_MARKER_PREFIX = "calfit_workspace_id=";

  private final GoogleCalendarApi googleCalendarApi;
  private final ZoneId appZoneId;
  private final ConcurrentMap<String, ReentrantLock> workspaceLocks = new ConcurrentHashMap<>();

  // 캘린더에 하나의 일정 추가
  @Override
  public String resolveWorkspaceCalendarId(
      String accessToken, String workspaceId, String workspaceName) {
    if (!hasText(workspaceId)) {
      throw new IllegalStateException("GOOGLE_WORKSPACE_ID_MISSING");
    }

    String bearerToken = "Bearer " + accessToken;
    String workspaceMarker = buildWorkspaceMarker(workspaceId);

    return runWithWorkspaceLock(
        workspaceId,
        () -> resolveWorkspaceCalendarIdWithLock(bearerToken, workspaceMarker, workspaceName));
  }

  private String resolveWorkspaceCalendarIdWithLock(
      String bearerToken, String workspaceMarker, String workspaceName) {
    try {
      String existingCalendarId = findWorkspaceCalendarId(bearerToken, workspaceMarker);
      if (hasText(existingCalendarId)) {
        return existingCalendarId;
      }

      GoogleCalendarCreateRequestDto requestDto =
          new GoogleCalendarCreateRequestDto(
              buildWorkspaceCalendarName(workspaceName), workspaceMarker, appZoneId.getId());

      GoogleCalendarCreateResponseDto responseDto =
          googleCalendarApi.createCalendar(bearerToken, requestDto);

      if (responseDto == null || !hasText(responseDto.id())) {
        throw new IllegalStateException("GOOGLE_CALENDAR_CREATE_FAILED");
      }

      String canonicalCalendarId = findWorkspaceCalendarId(bearerToken, workspaceMarker);
      if (hasText(canonicalCalendarId)) {
        return canonicalCalendarId;
      }

      return responseDto.id();
    } catch (FeignException e) {
      if (e.status() == 400 || e.status() == 401 || e.status() == 403) {
        throw new IllegalStateException("GOOGLE_OAUTH_ERROR", e);
      }
      if (e.status() >= 500) {
        throw new IllegalStateException("GOOGLE_TEMPORARY_ERROR", e);
      }
      throw new IllegalStateException("GOOGLE_CALENDAR_CREATE_FAILED", e);
    }
  }

  // 캘린더 목록에서 현재 워크스페이스 마커와 일치하는 캘린더를 찾음
  private String findWorkspaceCalendarId(String bearerToken, String workspaceMarker) {
    List<String> matchedCalendarIds = new ArrayList<>();
    String pageToken = null;

    do {
      GoogleCalendarListResponseDto responseDto =
          googleCalendarApi.listCalendars(bearerToken, pageToken, false, null);

      if (responseDto != null && responseDto.items() != null) {
        for (GoogleCalendarListResponseDto.Item item : responseDto.items()) {
          if (item == null || Boolean.TRUE.equals(item.deleted())) {
            continue;
          }
          if (!hasText(item.id()) || !hasText(item.description())) {
            continue;
          }
          if (item.description().contains(workspaceMarker)) {
            matchedCalendarIds.add(item.id());
          }
        }
      }

      pageToken = responseDto != null ? responseDto.nextPageToken() : null;
    } while (hasText(pageToken));

    return findCanonicalCalendarId(matchedCalendarIds);
  }

  private String findCanonicalCalendarId(List<String> calendarIds) {
    return calendarIds.stream().filter(this::hasText).sorted().findFirst().orElse(null);
  }

  private String runWithWorkspaceLock(String workspaceId, Supplier<String> action) {
    ReentrantLock lock = workspaceLocks.computeIfAbsent(workspaceId, key -> new ReentrantLock());
    lock.lock();
    try {
      return action.get();
    } finally {
      lock.unlock();
      if (!lock.hasQueuedThreads()) {
        workspaceLocks.remove(workspaceId, lock);
      }
    }
  }

  // 워크스페이스 식별 마커 문자열을 생성
  private String buildWorkspaceMarker(String workspaceId) {
    return WORKSPACE_MARKER_PREFIX + workspaceId;
  }

  // 워크스페이스 이름 기반의 구글 캘린더 이름을 생성
  private String buildWorkspaceCalendarName(String workspaceName) {
    String resolvedWorkspaceName = hasText(workspaceName) ? workspaceName : DEFAULT_WORKSPACE_NAME;
    return CALENDAR_NAME_PREFIX + resolvedWorkspaceName;
  }

  // 캘린더에 하나의 일정을 추가
  @Override
  public GoogleCalendarClientResult createEvent(
      String accessToken,
      String calendarId,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isAllDay) {

    validateCalendarId(calendarId);

    GoogleEventRequestDto requestDto =
        createEventRequest(summary, description, start, end, isAllDay);

    String bearerToken = "Bearer " + accessToken;
    GoogleEventResponseDto responseDto =
        googleCalendarApi.createEvent(bearerToken, calendarId, requestDto);

    return responseDto.toResult();
  }

  // 캘린더에 하나의 일정 수정
  @Override
  public GoogleCalendarClientResult updateEvent(
      String accessToken,
      String calendarId,
      String googleEventId,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isAllDay) {

    validateCalendarId(calendarId);

    GoogleEventRequestDto requestDto =
        createEventRequest(summary, description, start, end, isAllDay);

    String bearerToken = "Bearer " + accessToken;
    GoogleEventResponseDto responseDto =
        googleCalendarApi.updateEvent(bearerToken, calendarId, googleEventId, requestDto);

    return responseDto.toResult();
  }

  // 캘린더 일정 삭제
  @Override
  public void deleteEvent(String accessToken, String calendarId, String googleEventId) {
    validateCalendarId(calendarId);

    String bearerToken = "Bearer " + accessToken;
    googleCalendarApi.deleteEvent(bearerToken, calendarId, googleEventId);
  }

  // 캘린더에 있는 일정 가져오기
  @Override
  public GoogleCalendarSyncResult syncEvent(
      String accessToken, String calendarId, String syncToken) {
    validateCalendarId(calendarId);

    String bearerToken = "Bearer " + accessToken;
    List<SyncEventModel> allEvents = new ArrayList<>();
    String pageToken = null;
    String finalSyncToken = null;

    try {
      do {
        GoogleCalendarSyncResponseDto responseDto =
            googleCalendarApi.syncEvent(bearerToken, calendarId, syncToken, pageToken);

        GoogleCalendarSyncResult partialResult = responseDto.toResult();
        if (partialResult.items() != null) {
          allEvents.addAll(partialResult.items());
        }

        // pageToken 이 없을 때까지 모든 일정 조회
        pageToken = responseDto.nextPageToken();
        if (pageToken == null) {
          finalSyncToken = responseDto.nextSyncToken();
        }
      } while (pageToken != null);

      return new GoogleCalendarSyncResult(allEvents, finalSyncToken);

    } catch (FeignException.Gone e) {
      throw new IllegalStateException("GOOGLE_SYNC_TOKEN_EXPIRED", e);
    }
  }

  // 구글 이벤트 변경 알림을 받기 위한 watch 채널 생성
  @Override
  public GoogleCalendarWatchResult watchEvents(
      String accessToken,
      String calendarId,
      String callbackUrl,
      String channelToken,
      Long ttlSeconds) {
    validateCalendarId(calendarId);

    if (callbackUrl == null || callbackUrl.isBlank()) {
      throw new IllegalStateException("GOOGLE_WATCH_CALLBACK_URL_MISSING");
    }

    String bearerToken = "Bearer " + accessToken;
    GoogleCalendarWatchRequestDto requestDto =
        new GoogleCalendarWatchRequestDto(
            UUID.randomUUID().toString(),
            "web_hook",
            callbackUrl,
            channelToken,
            createWatchParams(ttlSeconds));

    try {
      GoogleCalendarWatchResponseDto responseDto =
          googleCalendarApi.watchEvents(bearerToken, calendarId, requestDto);
      return responseDto.toResult();
    } catch (FeignException e) {
      if (e.status() == 400 || e.status() == 401 || e.status() == 403) {
        throw new IllegalStateException("GOOGLE_WATCH_REGISTRATION_FAILED", e);
      }
      if (e.status() >= 500) {
        throw new IllegalStateException("GOOGLE_TEMPORARY_ERROR", e);
      }
      throw new IllegalStateException("GOOGLE_OAUTH_ERROR", e);
    }
  }

  // 캘린더 ID 유효성을 검사
  private void validateCalendarId(String calendarId) {
    if (!hasText(calendarId)) {
      throw new IllegalStateException("GOOGLE_CALENDAR_ID_MISSING");
    }
  }

  private GoogleEventRequestDto createEventRequest(
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean isAllDay) {
    GoogleEventRequestDto.EventDateTime startDto = createEventDateTime(start, isAllDay);
    GoogleEventRequestDto.EventDateTime endDto = createEventDateTime(end, isAllDay);

    return new GoogleEventRequestDto(summary, description, startDto, endDto);
  }

  // 이벤트 시간 변환 헬퍼 메소드
  private GoogleEventRequestDto.EventDateTime createEventDateTime(
      ZonedDateTime time, boolean isAllDay) {
    String timeZoneId = time.getZone().getId();

    if (isAllDay) {
      return new GoogleEventRequestDto.EventDateTime(
          null, time.toLocalDate().toString(), timeZoneId);
    } else {
      return new GoogleEventRequestDto.EventDateTime(
          time.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), null, timeZoneId);
    }
  }

  // watch 채널 생성 요청에 전달할 TTL 파라미터를 구성
  private Map<String, String> createWatchParams(Long ttlSeconds) {
    if (ttlSeconds == null || ttlSeconds <= 0) {
      return Map.of();
    }

    return Map.of("ttl", String.valueOf(ttlSeconds));
  }

  // 문자열이 null 또는 공백인지 확인
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
