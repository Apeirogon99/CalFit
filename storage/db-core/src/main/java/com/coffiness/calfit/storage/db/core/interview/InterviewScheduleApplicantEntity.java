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
    name = "interview_schedule_applicants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"interview_schedule_id", "applicant_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduleApplicantEntity extends TenantBaseEntity {

  // 인터뷰 일정 ID
  @Column(name = "interview_schedule_id", nullable = false)
  private Long interviewScheduleId;

  // 지원자 ID
  @Column(name = "applicant_id", nullable = false)
  private Long applicantId;

  public InterviewScheduleApplicantEntity(Long interviewScheduleId, Long applicantId) {
    this.interviewScheduleId = interviewScheduleId;
    this.applicantId = applicantId;
  }
}
