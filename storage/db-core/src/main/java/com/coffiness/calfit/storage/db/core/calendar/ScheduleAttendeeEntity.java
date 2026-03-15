package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 일정-참석자간 맵핑 테이블
 * */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "schedule_attendees")
public class ScheduleAttendeeEntity extends TenantBaseEntity {

  @Column(name = "schedule_id", nullable = false)
  private Long scheduleId;

  @Column(name = "attendee_id", nullable = false)
  private Long attendeeId;

  @Builder
  public ScheduleAttendeeEntity(String tenantId, Long scheduleId, Long attendeeId) {
    super(tenantId);
    this.scheduleId = scheduleId;
    this.attendeeId = attendeeId;
  }
}
