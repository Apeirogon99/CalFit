package com.coffiness.calfit.storage.db.core.meetingRoom;

import com.coffiness.calfit.core.enums.MeetingRoomActionType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_room_histories")
@NoArgsConstructor
@Getter
public class MeetingRoomHistoryEntity extends TenantBaseEntity {

  // 회의실 id
  @Column(name = "meeting_room_id", nullable = false)
  private Long meetingRoomId;

  // 회의실 예약 id(예약 관련이면)
  @Column(name = "meeting_room_resevation_id", nullable = true)
  private Long meetingRoomReservationId;

  // 행위를 한 사용자 id
  @Column(name = "actor_id", nullable = false)
  private Long actorId;

  // 이유
  @Column(name = "reason", nullable = true)
  private String reason;

  // 행위가 발생한 시간
  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  // 행위 타입
  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false)
  private MeetingRoomActionType actionType;

  @Column(name = "detail_json", columnDefinition = "json")
  private String detailJson;

  @Builder
  public MeetingRoomHistoryEntity(
      Long meetingRoomId,
      Long meetingRoomReservationId,
      Long actorId,
      String reason,
      LocalDateTime occurredAt,
      MeetingRoomActionType actionType,
      String detailJson) {
    this.meetingRoomId = meetingRoomId;
    this.meetingRoomReservationId = meetingRoomReservationId;
    this.actorId = actorId;
    this.reason = reason;
    this.occurredAt = occurredAt;
    this.actionType = actionType;
    this.detailJson = detailJson;
  }
}
