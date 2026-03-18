package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import java.time.LocalDateTime;
import java.util.List;

public record ApplicationDetailResponse(
    Long applicationId,
    Long applicantId,
    Long recruitmentId,
    Long recruitmentProcessId,
    Long templateId,
    String name,
    Gender gender,
    LocalDateTime birthDate,
    String phone,
    String email,
    String shortBio,
    String formFields,
    List<ApplicationAnswerResponse> answers,
    List<ApplicationFileResponse> files) {

  public static ApplicationDetailResponse from(
      ApplicationEntity entity,
      String shortBio,
      List<ApplicationAnswerResponse> answers,
      List<ApplicationFileResponse> files) {
    return new ApplicationDetailResponse(
        entity.getId(),
        entity.getApplicantId(),
        entity.getRecruitmentId(),
        entity.getRecruitmentProcessId(),
        entity.getTemplateId(),
        entity.getName(),
        entity.getGender(),
        entity.getBirthDate(),
        entity.getPhone(),
        entity.getEmail(),
        shortBio,
        entity.getFormFields(),
        answers,
        files);
  }
}
