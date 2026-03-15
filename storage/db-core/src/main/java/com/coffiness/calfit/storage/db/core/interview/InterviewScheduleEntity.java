package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduleEntity extends TenantBaseEntity {

  // 채용 ID
  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  // 채용 단계 ID
  @Column(name = "recruitment_stage_id")
  private Long recruitmentStageId;

  // 회의실 ID
  @Column(name = "meeting_room_id", nullable = false)
  private Long meetingRoomId;

  // 스케줄 일정
  @Column(name = "scheduled_at", nullable = false)
  private LocalDateTime scheduledAt;

  // 진행시간
  @Column(name = "durationMinutes", nullable = false)
  private Integer durationMinutes;

  // 인터뷰 내용
  @Column(name = "memo", nullable = false)
  private String memo;

  // 인터뷰 상태
  @Enumerated(EnumType.STRING)
  @Column(name = "interview_status", nullable = false)
  private InterviewStatus interviewStatus;

  public InterviewScheduleEntity(
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      InterviewStatus interviewStatus) {
    this.recruitmentId = recruitmentId;
    this.recruitmentStageId = recruitmentStageId;
    this.meetingRoomId = meetingRoomId;
    this.scheduledAt = scheduledAt;
    this.durationMinutes = durationMinutes;
    this.memo = memo;
    this.interviewStatus = interviewStatus;
  }

  // 확정 상태로 변경
  public void confirm() {
    this.interviewStatus = InterviewStatus.CONFIRMED;
  }

  // 취소 상태로 변경
  public void cancel() {
    this.interviewStatus = InterviewStatus.CANCELLED;
  }

  // 완료 상태로 변경
  public void complete() {
    this.interviewStatus = InterviewStatus.COMPLETED;
  }

  // 대기 상태로 변경
  public void pending() {
    this.interviewStatus = InterviewStatus.PENDING;
  }

  // 면접 종료 시간 계산
  public LocalDateTime getEndTime() {
    return this.scheduledAt.plusMinutes(this.durationMinutes);
  }
}
