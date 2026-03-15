package com.coffiness.calfit.storage.db.core.automation;

import com.coffiness.calfit.core.enums.AutomationActionType;
import com.coffiness.calfit.core.enums.AutomationEventStatus;
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
@Table(name = "automation_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AutomationEventEntity extends TenantBaseEntity {

  @Column(name = "application_id", nullable = false)
  private Long applicationId;

  @Column(name = "rule_id", nullable = false)
  private Long ruleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false)
  private AutomationActionType actionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_status", nullable = false)
  private AutomationEventStatus eventStatus;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Builder
  public AutomationEventEntity(
      Long applicationId,
      Long ruleId,
      AutomationActionType actionType,
      AutomationEventStatus eventStatus,
      String errorMessage) {
    this.applicationId = applicationId;
    this.ruleId = ruleId;
    this.actionType = actionType;
    this.eventStatus = eventStatus;
    this.errorMessage = errorMessage;
  }

  public void markSuccess() {
    this.eventStatus = AutomationEventStatus.SUCCESS;
    this.errorMessage = null;
  }

  public void markFailed(String errorMessage) {
    this.eventStatus = AutomationEventStatus.FAILED;
    this.errorMessage = errorMessage;
  }
}
