package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.interview.InterviewerInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentDetailResponse(
    Long id,
    String title,
    String contents,
    String leadGroupName,
    Long leadGroupId,
    List<Long> referenceGroupIds,
    int targetCount,
    Long applicationTemplateId,
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    LocalDateTime startDate,
    LocalDateTime endDate,
    int totalApplicants,
    int processingInterview,
    int dDay,
    String shareUrl,
    RecruitmentStatus recruitmentStatus,
    List<InterviewerInfo> interviewers,
    List<StageDetail> stages) {

  public static RecruitmentDetailResponse from(RecruitmentDetailInfo info) {
    return new RecruitmentDetailResponse(
        info.id(),
        info.title(),
        info.contents(),
        info.leadGroupName(),
        info.leadGroupId(),
        info.referenceGroupIds(),
        info.targetCount(),
        info.applicationTemplateId(),
        info.careerType(),
        info.minExperienceYears(),
        info.maxExperienceYears(),
        info.startDate(),
        info.endDate(),
        info.totalApplicants(),
        info.processingInterview(),
        info.dDay(),
        info.shareUrl(),
        info.recruitmentStatus(),
        info.interviewers().stream()
            .map(i -> new InterviewerInfo(i.userId(), i.name(), i.enabled()))
            .toList(),
        info.stages().stream()
            .map(
                stage ->
                    new StageDetail(
                        stage.id(), stage.stageName(), stage.stageStep(), stage.stageType()))
            .toList());
  }

  public record StageDetail(
      Long id,
      String stageName,
      Integer stageStep,
      com.coffiness.calfit.core.enums.RecruitmentStageType stageType) {}
}
