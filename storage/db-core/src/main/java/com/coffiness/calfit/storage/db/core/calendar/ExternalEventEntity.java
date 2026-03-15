package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.core.enums.EventStatus;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

/*
 * 구글 캘린더 내부 개별 일정 엔티티
 * */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "external_event",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_external_event_calendar_google",
          columnNames = {"tenant_id", "external_calendar_id", "google_event_id"})
    })
public class ExternalEventEntity extends TenantBaseEntity {

  // ExternalCalendar Id
  @Column(name = "external_calendar_id", nullable = false)
  private Long externalCalendarId;

  // 구글 캘린더 고유 일정 ID
  @Column(name = "google_event_id", nullable = false)
  private String googleEventId;

  @Column(name = "title", length = 255)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @TimeZoneStorage(TimeZoneStorageType.COLUMN)
  @TimeZoneColumn(name = "start_time_tz")
  @Column(name = "start_time", nullable = false)
  private ZonedDateTime startTime;

  @TimeZoneStorage(TimeZoneStorageType.COLUMN)
  @TimeZoneColumn(name = "end_time_tz")
  @Column(name = "end_time", nullable = false)
  private ZonedDateTime endTime;

  // 종일 일정 여부
  @Column(name = "is_all_day", nullable = false)
  private boolean isAllDay;

  // 일정 상태 (취소됨은 시간 계산 시 제외)
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private EventStatus status;

  @Column(name = "is_busy", nullable = false)
  private boolean isBusy;

  @Builder
  public ExternalEventEntity(
      String tenantId,
      Long externalCalendarId,
      String googleEventId,
      String title,
      String description,
      ZonedDateTime startTime,
      ZonedDateTime endTime,
      boolean isAllDay,
      EventStatus status,
      boolean isBusy) {
    super(tenantId);
    this.externalCalendarId = externalCalendarId;
    this.googleEventId = googleEventId;
    this.title = title;
    this.description = description;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isAllDay = isAllDay;
    this.status = status;
    this.isBusy = isBusy;
  }
}
