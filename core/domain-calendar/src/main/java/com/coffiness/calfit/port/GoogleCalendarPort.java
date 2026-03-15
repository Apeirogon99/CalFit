package com.coffiness.calfit.port;

import com.coffiness.calfit.model.GoogleCalendarClientResult;
import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import com.coffiness.calfit.model.GoogleCalendarWatchResult;
import java.time.ZonedDateTime;

public interface GoogleCalendarPort {

  // 워크스페이스 전용 캘린더를 조회하고 없으면 생성한 뒤 캘린더 ID를 반환
  String resolveWorkspaceCalendarId(String accessToken, String workspaceId, String workspaceName);

  GoogleCalendarClientResult createEvent(
      String accessToken,
      String calendarId,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean allDay);

  GoogleCalendarClientResult updateEvent(
      String accessToken,
      String calendarId,
      String googleEventId,
      String summary,
      String description,
      ZonedDateTime start,
      ZonedDateTime end,
      boolean allDay);

  void deleteEvent(String accessToken, String calendarId, String googleEventId);

  GoogleCalendarSyncResult syncEvent(String accessToken, String calendarId, String syncToken);

  // 구글 이벤트 변경 알림 수신을 위한 watch 채널을 생성
  GoogleCalendarWatchResult watchEvents(
      String accessToken,
      String calendarId,
      String callbackUrl,
      String channelToken,
      Long ttlSeconds);
}
