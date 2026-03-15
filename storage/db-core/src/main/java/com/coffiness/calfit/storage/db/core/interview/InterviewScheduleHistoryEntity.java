package com.coffiness.calfit.storage.db.core.interview;

import com.coffiness.calfit.core.enums.InterviewEventType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "interview_schedule_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewScheduleHistoryEntity extends TenantBaseEntity {

  // 인터뷰 일정 ID
  @Column(name = "interview_schedule_id", nullable = false)
  private Long interviewScheduleId;

  // 인터뷰 이벤트
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  private InterviewEventType eventType;

  // 이벤트 데이터
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "event_data", columnDefinition = "json")
  private Map<String, Object> eventData;

  // 생성자
  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Builder
  public InterviewScheduleHistoryEntity(
      Long interviewScheduleId,
      InterviewEventType eventType,
      Map<String, Object> eventData,
      Long createdBy) {
    this.interviewScheduleId = interviewScheduleId;
    this.eventType = eventType;
    this.eventData = eventData;
    this.createdBy = createdBy;
  }
}
