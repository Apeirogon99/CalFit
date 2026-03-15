package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 채용 공고 면접관 엔티티
 * */
@Entity
@Table(name = "recruitment_interviewers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RecruitmentInterviewerEntity extends TenantBaseEntity {

  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Builder
  public RecruitmentInterviewerEntity(Long recruitmentId, Long userId) {
    this.recruitmentId = recruitmentId;
    this.userId = userId;
  }
}
