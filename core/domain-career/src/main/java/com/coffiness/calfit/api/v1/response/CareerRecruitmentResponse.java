package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.domain.recruitment.OpenRecruitmentInfo;
import java.time.LocalDateTime;

public record CareerRecruitmentResponse(
    Long recruitmentId,
    String title,
    String contents,
    CareerType careerType,
    int targetCount,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    LocalDateTime startDate,
    LocalDateTime endDate) {

  public static CareerRecruitmentResponse from(OpenRecruitmentInfo info) {
    return new CareerRecruitmentResponse(
        info.recruitmentId(),
        info.title(),
        info.contents(),
        info.careerType(),
        info.targetCount(),
        info.minExperienceYears(),
        info.maxExperienceYears(),
        info.startDate(),
        info.endDate());
  }
}
