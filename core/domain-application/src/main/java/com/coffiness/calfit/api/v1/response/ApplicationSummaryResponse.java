package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import java.time.LocalDateTime;

public record ApplicationSummaryResponse(
    Long applicationId,
    Long applicantId,
    Long recruitmentId,
    Long templateId,
    String name,
    String email,
    String phone,
    LocalDateTime createdAt) {

  public static ApplicationSummaryResponse from(ApplicationEntity entity) {
    return new ApplicationSummaryResponse(
        entity.getId(),
        entity.getApplicantId(),
        entity.getRecruitmentId(),
        entity.getTemplateId(),
        entity.getName(),
        entity.getEmail(),
        entity.getPhone(),
        entity.getCreatedAt());
  }
}
