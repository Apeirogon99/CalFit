package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "interview_schedule_interviewers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"interview_schedule_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduleInterviewerEntity extends TenantBaseEntity {

  // 인터뷰 일정 ID
  @Column(name = "interview_schedule_id", nullable = false)
  private Long interviewScheduleId;

  // 면잡관 ID
  @Column(name = "user_id", nullable = false)
  private Long userId;

  public InterviewScheduleInterviewerEntity(Long interviewScheduleId, Long userId) {
    this.interviewScheduleId = interviewScheduleId;
    this.userId = userId;
  }
}
