package com.coffiness.calfit.model;

import java.time.ZonedDateTime;
import java.util.List;

public record GoogleCalendarSyncResult(List<SyncEventModel> items, String nextSyncToken) {
  public record SyncEventModel(
      String googleEventId,
      String status,
      String summary,
      String description,
      ZonedDateTime startTime,
      ZonedDateTime endTime,
      boolean allDay,
      boolean free // 한가함 여부
      ) {}
}
