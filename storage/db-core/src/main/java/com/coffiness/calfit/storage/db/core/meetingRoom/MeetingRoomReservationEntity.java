package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_room_reservations")
@NoArgsConstructor
@Getter
public class MeetingRoomReservationEntity extends TenantBaseEntity {

  // 회의실 id
  @Column(name = "meeting_room_id", nullable = false)
  private Long meetingRoomId;

  // 사용자 id
  @Column(name = "user_id", nullable = false)
  private Long userId;

  // 면접 일정 id (면접 예약이 아닌 경우 null 가능)
  @Column(name = "interview_schedule_id", nullable = true)
  private Long interviewScheduleId;

  // 예약한 시간(시작)
  @Column(name = "start_datetime", nullable = false)
  private LocalDateTime startDatetime;

  // 예약한 시작(끝)
  @Column(name = "end_datetime", nullable = false)
  private LocalDateTime endDatetime;

  // 회의실 예약 상태(예약됨, 취소됨)
  @Enumerated(EnumType.STRING)
  @Column(name = "reservation_status", nullable = false)
  private MeetingRoomStatus reservationStatus;

  @Builder
  public MeetingRoomReservationEntity(
      Long meetingRoomId,
      Long userId,
      Long interviewScheduleId,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime,
      MeetingRoomStatus reservationStatus) {
    this.meetingRoomId = meetingRoomId;
    this.userId = userId;
    this.interviewScheduleId = interviewScheduleId;
    this.startDatetime = startDatetime;
    this.endDatetime = endDatetime;
    this.reservationStatus = reservationStatus;
  }

  public void cancel() {
    this.reservationStatus = MeetingRoomStatus.DELETED;
  }

  public void activate() {
    this.reservationStatus = MeetingRoomStatus.ACTIVE;
  }

  public void expire() {
    this.reservationStatus = MeetingRoomStatus.EXPIRED;
  }
}
