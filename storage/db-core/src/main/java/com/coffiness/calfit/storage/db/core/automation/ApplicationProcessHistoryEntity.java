package com.coffiness.calfit.storage.db.core.automation;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application_process_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ApplicationProcessHistoryEntity extends TenantBaseEntity {

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  @Column(name = "from_recruitment_process_id")
  private Long fromRecruitmentProcessId;

  @Column(name = "to_recruitment_process_id", nullable = false)
  private Long toRecruitmentProcessId;

  @Column(name = "actor_id", nullable = false)
  private Long actorId;

  @Builder
  public ApplicationProcessHistoryEntity(
      Long applicationId,
      Long fromRecruitmentProcessId,
      Long toRecruitmentProcessId,
      Long actorId) {
    this.applicationId = applicationId;
    this.fromRecruitmentProcessId = fromRecruitmentProcessId;
    this.toRecruitmentProcessId = toRecruitmentProcessId;
    this.actorId = actorId;
  }
}
