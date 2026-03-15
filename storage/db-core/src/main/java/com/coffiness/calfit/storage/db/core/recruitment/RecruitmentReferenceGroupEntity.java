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
 * 채용 공고 참조 조직 맵핑 엔티티
 * */
@Entity
@Table(name = "recruitment_reference_teams")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RecruitmentReferenceGroupEntity extends TenantBaseEntity {

  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  @Column(name = "group_id", nullable = false)
  private Long groupId;

  @Builder
  public RecruitmentReferenceGroupEntity(Long recruitmentId, Long groupId) {
    this.recruitmentId = recruitmentId;
    this.groupId = groupId;
  }
}
