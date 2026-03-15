package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record Recruitment(
    Long id,
    Long creatorId,
    String title,
    String contents,
    RecruitmentStatus recruitmentStatus,
    int targetCount,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Long applicationTemplateId,
    CareerType careerType,
    Integer minExperienceYears,
    Integer maxExperienceYears,
    Long leadGroupId,
    List<Long> referenceGroupIds,
    List<Long> interviewerIds,
    List<RecruitmentStage> stages) {
  public Recruitment {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("채용 제목은 필수입니다.");
    }
    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("채용 일정(시작/종료)은 필수입니다.");
    }
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("마감일이 시작일보다 빠를 수 없습니다.");
    }
    if (targetCount < 1) {
      throw new IllegalArgumentException("채용 목표 인원은 최소 1명 이상이어야 합니다.");
    }
    if (stages == null || stages.isEmpty()) {
      throw new IllegalArgumentException("최소 1개의 채용 단계가 필요합니다.");
    }

    if (careerType == CareerType.EXPERIENCED) {
      if (minExperienceYears == null && maxExperienceYears == null) {
        throw new IllegalArgumentException("경력직 채용의 경우 경력 연차 범위가 필요합니다.");
      }
    }
    if (careerType == null) {
      throw new IllegalArgumentException("경력 구분 지정은 필수입니다.");
    }

    if (careerType == CareerType.EXPERIENCED) {
      if (minExperienceYears == null || maxExperienceYears == null) {
        throw new IllegalArgumentException("경력직 채용의 경우 최소 및 최대 경력 연차가 모두 필요합니다.");
      }
      if (minExperienceYears < 0 || maxExperienceYears < 0) {
        throw new IllegalArgumentException("경력 연차는 0년 이상이어야 합니다.");
      }
      if (minExperienceYears > maxExperienceYears) {
        throw new IllegalArgumentException("최소 경력 연차는 최대 경력 연차보다 클 수 없습니다.");
      }
    } else {
      if (minExperienceYears != null || maxExperienceYears != null) {
        throw new IllegalArgumentException("경력직이 아닌 채용은 경력 연차 범위를 가질 수 없습니다.");
      }
    }
  }

  // 채용 공고 상세 수정
  public Recruitment updateDetails(
      String newTitle,
      String newContents,
      int newTargetCount,
      LocalDateTime newStartDate,
      LocalDateTime newEndDate,
      Long newApplicationTemplateId,
      CareerType newCareerType,
      Integer newMinExperienceYears,
      Integer newMaxExperienceYears,
      Long newLeadGroupId,
      List<Long> newReferenceGroupIds,
      List<Long> newInterviewerIds,
      List<RecruitmentStage> newStages) {
    return new Recruitment(
        this.id,
        this.creatorId,
        newTitle,
        newContents,
        this.recruitmentStatus,
        newTargetCount,
        newStartDate,
        newEndDate,
        newApplicationTemplateId,
        newCareerType,
        newMinExperienceYears,
        newMaxExperienceYears,
        newLeadGroupId,
        newReferenceGroupIds,
        newInterviewerIds,
        newStages);
  }

  // 채용 공고 상태만 변경
  public Recruitment updateStatus(RecruitmentStatus newStatus) {
    return new Recruitment(
        this.id,
        this.creatorId,
        this.title,
        this.contents,
        newStatus,
        this.targetCount,
        this.startDate,
        this.endDate,
        this.applicationTemplateId,
        this.careerType,
        this.minExperienceYears,
        this.maxExperienceYears,
        this.leadGroupId,
        this.referenceGroupIds,
        this.interviewerIds,
        this.stages);
  }

  // 채용 공고의 면접관 목록만 별도로 교체
  public Recruitment updateInterviewers(List<Long> newInterviewerIds) {
    if (newInterviewerIds == null || newInterviewerIds.isEmpty()) {
      throw new IllegalArgumentException("면접관은 최소 1명 이상이어야 합니다.");
    }

    return new Recruitment(
        this.id,
        this.creatorId,
        this.title,
        this.contents,
        this.recruitmentStatus,
        this.targetCount,
        this.startDate,
        this.endDate,
        this.applicationTemplateId,
        this.careerType,
        this.minExperienceYears,
        this.maxExperienceYears,
        this.leadGroupId,
        this.referenceGroupIds,
        List.copyOf(new ArrayList<>(newInterviewerIds)),
        this.stages);
  }

  // DRAFT 공고를 현재 시각 기준으로 즉시 게시 상태로 전환
  public Recruitment publishNow(LocalDateTime currentTime) {
    if (currentTime == null) {
      throw new IllegalArgumentException("현재 시간이 필요합니다.");
    }
    if (this.recruitmentStatus == RecruitmentStatus.OPEN) {
      throw new IllegalArgumentException("이미 게시된 채용 공고입니다.");
    }
    if (this.recruitmentStatus == RecruitmentStatus.CLOSED) {
      throw new IllegalArgumentException("마감된 채용 공고는 게시할 수 없습니다.");
    }
    if (this.endDate != null && !this.endDate.isAfter(currentTime)) {
      throw new IllegalArgumentException("이미 마감 시각이 지난 채용 공고는 게시할 수 없습니다.");
    }

    LocalDateTime publishedStartDate =
        this.startDate != null && !this.startDate.isAfter(currentTime)
            ? this.startDate
            : currentTime;

    return new Recruitment(
        this.id,
        this.creatorId,
        this.title,
        this.contents,
        RecruitmentStatus.OPEN,
        this.targetCount,
        publishedStartDate,
        this.endDate,
        this.applicationTemplateId,
        this.careerType,
        this.minExperienceYears,
        this.maxExperienceYears,
        this.leadGroupId,
        this.referenceGroupIds,
        this.interviewerIds,
        this.stages);
  }

  // 채용 공고 수정 정책 로직
  public void validateUpdatable(LocalDateTime currentTime) {
    if (currentTime == null) {
      throw new IllegalArgumentException("현재 시간이 필요합니다.");
    }
    if (this.startDate != null && !currentTime.isBefore(this.startDate)) {
      throw new IllegalArgumentException("이미 게시가 시작된 채용 공고는 수정할 수 없습니다.");
    }

    if (this.recruitmentStatus() == RecruitmentStatus.CLOSED) {
      throw new IllegalArgumentException("마감된 공고는 수정할 수 없습니다.");
    }
  }
}
