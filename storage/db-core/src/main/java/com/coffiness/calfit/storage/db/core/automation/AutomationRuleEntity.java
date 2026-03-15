package com.coffiness.calfit.storage.db.core.automation;

import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationTriggerType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "automation_rules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AutomationRuleEntity extends TenantBaseEntity {

  @Column(name = "recruitment_id", nullable = false)
  private Long recruitmentId;

  @Column(name = "recruitment_process_id", nullable = false)
  private Long recruitmentProcessId;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false)
  private AutomationTriggerType triggerType;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false)
  private AutomationActionType actionType;

  @Column(name = "payload", columnDefinition = "JSON")
  private String payload;

  @Builder
  public AutomationRuleEntity(
      Long recruitmentId,
      Long recruitmentProcessId,
      AutomationTriggerType triggerType,
      AutomationActionType actionType,
      String payload) {
    this.recruitmentId = recruitmentId;
    this.recruitmentProcessId = recruitmentProcessId;
    this.triggerType = triggerType;
    this.actionType = actionType;
    this.payload = payload;
  }

  public void update(
      Long recruitmentProcessId,
      AutomationTriggerType triggerType,
      AutomationActionType actionType,
      String payload) {
    this.recruitmentProcessId = recruitmentProcessId;
    this.triggerType = triggerType;
    this.actionType = actionType;
    this.payload = payload;
  }
}
