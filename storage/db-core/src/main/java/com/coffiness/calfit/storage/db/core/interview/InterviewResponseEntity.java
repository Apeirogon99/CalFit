package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewResponseStatus;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewResponseEntity extends TenantBaseEntity {

  // 인터뷰 일정 ID
  @Column(name = "interview_schedule_id", nullable = false)
  private Long interviewScheduleId;

  // 면접관 ID
  @Column(name = "user_id", nullable = false)
  private Long userId;

  // 응답 상태
  @Enumerated(EnumType.STRING)
  @Column(name = "response_status", nullable = false)
  private InterviewResponseStatus responseStatus;

  // 응답 시간
  @Column(name = "responded_at", nullable = false)
  private LocalDateTime respondedAt;

  // 거절 사유
  @Column(name = "decline_reason", nullable = false)
  private String declineReason;

  public InterviewResponseEntity(
      Long interviewScheduleId,
      Long userId,
      InterviewResponseStatus responseStatus,
      LocalDateTime respondedAt,
      String declineReason) {
    this.interviewScheduleId = interviewScheduleId;
    this.userId = userId;
    this.responseStatus = responseStatus;
    this.respondedAt = respondedAt;
    this.declineReason = declineReason == null ? "" : declineReason;
  }

  public static InterviewResponseEntity accepted(Long interviewScheduleId, Long userId) {
    return new InterviewResponseEntity(
        interviewScheduleId, userId, InterviewResponseStatus.ACCEPTED, LocalDateTime.now(), "");
  }

  public void accept() {
    this.responseStatus = InterviewResponseStatus.ACCEPTED;
    this.respondedAt = LocalDateTime.now();
    this.declineReason = "";
  }

  public void decline(String reason) {
    this.responseStatus = InterviewResponseStatus.DECLINED;
    this.respondedAt = LocalDateTime.now();
    this.declineReason = reason == null ? "" : reason;
  }
}
