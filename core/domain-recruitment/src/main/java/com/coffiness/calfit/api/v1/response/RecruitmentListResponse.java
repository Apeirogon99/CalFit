package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.recruitment.AssigneeSummary;
import com.coffiness.calfit.domain.recruitment.RecruitmentListInfo;
import com.coffiness.calfit.domain.recruitment.StageSummary;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentListResponse(
    Long id,
    String title,
    String leadGroupName, // 부서명
    Long leadGroupId,
    List<Long> referenceGroupIds,
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    LocalDateTime startDate, // 시작일 (게시 전 여부 판단용)
    LocalDateTime endDate, // D-DAY 계산용 마감일
    RecruitmentStatus status, // 상태 뱃지 표시용 상태
    List<StageSummary> stages, // 단계별 정보
    List<AssigneeSummary> assignees // 하단 담당자 프로필
    ) {

  public static RecruitmentListResponse from(RecruitmentListInfo info) {
    return new RecruitmentListResponse(
        info.id(),
        info.title(),
        info.leadGroupName(),
        info.leadGroupId(),
        info.referenceGroupIds(),
        info.careerType(),
        info.minExperienceYears(),
        info.maxExperienceYears(),
        info.startDate(),
        info.endDate(),
        info.recruitmentStatus(),
        info.stages().stream()
            .map(s -> new StageSummary(s.id(), s.stageName(), s.applicantCount()))
            .toList(),
        info.assignees().stream().map(a -> new AssigneeSummary(a.userId(), a.name())).toList());
  }
}
