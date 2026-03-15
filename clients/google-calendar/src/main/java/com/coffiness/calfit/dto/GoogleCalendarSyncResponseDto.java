package com.coffiness.calfit.dto;

import com.coffiness.calfit.model.GoogleCalendarSyncResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/*
 * 전체 동기화용 DTO
 * */
public record GoogleCalendarSyncResponseDto(
    List<Item> items, String nextSyncToken, String nextPageToken) {

  public record Item(
      String id,
      String status,
      String summary,
      String description,
      String transparency, // 한가함 여부
      EventDateTime start,
      EventDateTime end) {}

  public record EventDateTime(
      String dateTime, // 일반 일정
      String date, // 종일 일정
      String timeZone) {}

  public GoogleCalendarSyncResult toResult() {
    List<Item> safeItems = this.items() != null ? this.items() : List.of();
    return new GoogleCalendarSyncResult(
        safeItems.stream()
            .map(
                item -> {
                  boolean isAllDay = item.start() != null && item.start().date() != null;

                  boolean isFree = "transparent".equals(item.transparency());

                  return new GoogleCalendarSyncResult.SyncEventModel(
                      item.id(),
                      item.status(),
                      item.summary(),
                      item.description(),
                      parseTime(item.start()),
                      parseTime(item.end()),
                      isAllDay,
                      isFree);
                })
            .toList(),
        this.nextSyncToken());
  }

  /*
   * ZonedDateTime으로 통일하는 헬퍼 메소드
   */
  private ZonedDateTime parseTime(EventDateTime eventDateTime) {
    if (eventDateTime == null) return null;

    if (eventDateTime.dateTime() != null) {
      return ZonedDateTime.parse(eventDateTime.dateTime());
    }

    if (eventDateTime.date() != null) {
      ZoneId zoneId =
          eventDateTime.timeZone() != null
              ? ZoneId.of(eventDateTime.timeZone())
              : ZoneId.of("Asia/Seoul");

      return LocalDate.parse(eventDateTime.date()).atStartOfDay(zoneId);
    }

    return null;
  }
}
