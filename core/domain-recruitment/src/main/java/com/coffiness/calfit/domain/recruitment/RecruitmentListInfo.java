package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentListInfo(
    Long id,
    String title,
    String leadGroupName,
    Long leadGroupId,
    List<Long> referenceGroupIds,
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    LocalDateTime startDate,
    LocalDateTime endDate,
    RecruitmentStatus recruitmentStatus,
    List<StageSummaryInfo> stages,
    List<AssigneeSummaryInfo> assignees) {
  public record StageSummaryInfo(Long id, String stageName, int applicantCount) {}

  public record AssigneeSummaryInfo(Long userId, String name) {}
}
