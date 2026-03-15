package com.coffiness.calfit.domain.application.event;

import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.support.event.DomainEvent;

public record ApplicationProcessChangedEvent(
    String tenantId,
    Long applicationId,
    String applicantName,
    Long recruitmentId,
    Long actorUserId,
    Long fromStageId,
    String fromStageName,
    RecruitmentStageType fromStageType,
    Long toStageId,
    String toStageName,
    RecruitmentStageType toStageType,
    long occurredAt)
    implements DomainEvent {

  public static ApplicationProcessChangedEvent of(
      String tenantId,
      Long applicationId,
      String applicantName,
      Long recruitmentId,
      Long actorUserId,
      Long fromStageId,
      String fromStageName,
      RecruitmentStageType fromStageType,
      Long toStageId,
      String toStageName,
      RecruitmentStageType toStageType) {
    return new ApplicationProcessChangedEvent(
        tenantId,
        applicationId,
        applicantName,
        recruitmentId,
        actorUserId,
        fromStageId,
        fromStageName,
        fromStageType,
        toStageId,
        toStageName,
        toStageType,
        System.currentTimeMillis());
  }
}
