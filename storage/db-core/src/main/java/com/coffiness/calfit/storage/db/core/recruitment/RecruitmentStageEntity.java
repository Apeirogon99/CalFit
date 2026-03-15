package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 채용 단계 엔티티
 * */
@Entity
@Table(name = "recruitment_stages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RecruitmentStageEntity extends TenantBaseEntity {

  // 채용 ID
  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  // 채용 단계명
  @Column(name = "stage_name", nullable = false)
  private String stageName;

  // 채용 단계 순서
  @Column(name = "stage_step", nullable = false)
  private Integer stageStep;

  // 채용 단계 유형
  @Enumerated(EnumType.STRING)
  @Column(name = "stage_type", nullable = false)
  private RecruitmentStageType stageType;

  @Builder
  public RecruitmentStageEntity(
      Long recruitmentId, String stageName, Integer stageStep, RecruitmentStageType stageType) {
    this.recruitmentId = recruitmentId;
    this.stageName = stageName;
    this.stageStep = stageStep;
    this.stageType = stageType;
  }
}
