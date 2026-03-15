package com.coffiness.calfit.storage.db.core.automation;

import com.coffiness.calfit.core.enums.AutomationTriggerType;
import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AutomationRuleRepository extends JpaRepository<AutomationRuleEntity, Long> {
  List<AutomationRuleEntity> findByRecruitmentIdAndRecruitmentProcessIdAndTriggerType(
      Long recruitmentId, Long recruitmentProcessId, AutomationTriggerType triggerType);

  List<AutomationRuleEntity> findByRecruitmentIdAndStatusOrderByCreatedAtAsc(
      Long recruitmentId, EntityStatus status);

  Optional<AutomationRuleEntity> findByIdAndStatus(Long id, EntityStatus status);

  boolean existsByRecruitmentIdAndRecruitmentProcessIdAndTriggerTypeAndActionTypeAndStatus(
      Long recruitmentId,
      Long recruitmentProcessId,
      AutomationTriggerType triggerType,
      com.coffiness.calfit.core.enums.AutomationActionType actionType,
      EntityStatus status);

  long countByStatus(EntityStatus status);

  @Query(
      "SELECT COUNT(DISTINCT e.recruitmentId) FROM AutomationRuleEntity e"
          + " WHERE e.status = :status")
  long countDistinctRecruitmentIdByStatus(@Param("status") EntityStatus status);
}
