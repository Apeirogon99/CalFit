package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.interview.InterviewerInfo;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentDetailInfo(
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
    List<RecruitmentStage> stages) {}
