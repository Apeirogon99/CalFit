package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/*
 * 채용 로그 엔티티
 * */
@Entity
@Table(name = "recruitment_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RecruitmentHistoryEntity extends TenantBaseEntity {

  // 채용 ID
  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  // 채용 단계 ID (Optional)
  @Column(name = "stage_id")
  private Long stageId;

  // 시행자 ID
  @Column(name = "actor_id", nullable = false)
  private Long actorId;

  // 시행 타입
  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false)
  private RecruitmentActionType recruitmentActionType;

  // 변경한 필드, 변경 전후 값의 JSON 필드
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "change_log", columnDefinition = "json")
  private Map<String, Object> changeLog;

  // 시행 이유(Optional)
  @Column(name = "reason")
  private String reason;

  @Builder
  public RecruitmentHistoryEntity(
      Long recruitmentId,
      Long stageId,
      Long actorId,
      RecruitmentActionType recruitmentActionType,
      Map<String, Object> changeLog,
      String reason) {
    this.recruitmentId = recruitmentId;
    this.stageId = stageId;
    this.actorId = actorId;
    this.recruitmentActionType = recruitmentActionType;
    this.changeLog = changeLog;
    this.reason = reason;
  }
}
