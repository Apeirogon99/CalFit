package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/*
 * 내부 일정 캘린더 엔티티
 * */
@Entity
@SQLRestriction("status = 'ACTIVE'")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "schedule",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_schedule_tenant_google_event",
          columnNames = {"tenant_id", "google_event_id", "status"})
    },
    indexes = {@Index(name = "idx_schedule_google_event_id", columnList = "google_event_id")})
public class ScheduleEntity extends TenantBaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "schedule_type", nullable = false, length = 20)
  private ScheduleType type;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Column(name = "is_all_day", nullable = false)
  private boolean isAllDay;

  @Column(name = "room_id")
  private Long roomId;

  @Column(name = "reservation_id")
  private Long reservationId;

  @Column(name = "is_busy")
  private boolean isBusy;

  @Column(name = "google_event_id", length = 255)
  private String googleEventId;

  @Column(name = "interview_schedule_id")
  private Long interviewScheduleId;

  @Builder
  public ScheduleEntity(
      String tenantId,
      Long userId,
      String title,
      String description,
      ScheduleType type,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isAllDay,
      Long roomId,
      Long reservationId,
      String googleEventId,
      boolean isBusy,
      Long interviewScheduleId) {
    super(tenantId);
    this.userId = userId;
    this.title = title;
    this.description = description;
    this.type = type;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isAllDay = isAllDay;
    this.roomId = roomId;
    this.reservationId = reservationId;
    this.googleEventId = googleEventId;
    this.isBusy = isBusy;
    this.interviewScheduleId = interviewScheduleId;
  }

  public void update(
      String title,
      String description,
      ScheduleType type,
      LocalDateTime startTime,
      LocalDateTime endTime,
      boolean isAllDay,
      Long roomId,
      Long reservationId,
      boolean isBusy) {

    if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
      throw new IllegalArgumentException("종료 시간은 시작 시간보다 이후여야 합니다.");
    }

    this.title = title;
    this.description = description;
    this.type = type;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isAllDay = isAllDay;
    this.roomId = roomId;
    this.reservationId = reservationId;
    this.isBusy = isBusy;
  }

  public void updateGoogleEventId(String googleEventId) {
    this.googleEventId = googleEventId;
  }
}
