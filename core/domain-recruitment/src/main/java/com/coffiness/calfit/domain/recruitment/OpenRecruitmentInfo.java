package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.CareerType;
import java.time.LocalDateTime;

public record OpenRecruitmentInfo(
    Long recruitmentId,
    String title,
    String contents,
    CareerType careerType,
    int targetCount,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    LocalDateTime startDate,
    LocalDateTime endDate) {}
