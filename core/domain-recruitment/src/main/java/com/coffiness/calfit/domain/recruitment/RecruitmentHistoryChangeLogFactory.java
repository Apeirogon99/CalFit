package com.coffiness.calfit.domain.recruitment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/*
 * 채용 이력 변경 로그 생성 팩토리
 * */
@Component
public class RecruitmentHistoryChangeLogFactory {

  private static final String RECRUITMENT_SCOPE = "RECRUITMENT";
  private static final String STAGE_SCOPE = "STAGE";
  private static final String INTERVIEWER_SCOPE = "INTERVIEWER";

  // 채용 공고 이력 변경 로그 생성
  public RecruitmentHistoryChangeLog createRecruitmentChangeLog(
      Recruitment beforeRecruitment, Recruitment afterRecruitment) {
    Map<String, Object> beforeSnapshot = toRecruitmentSnapshot(beforeRecruitment);
    Map<String, Object> afterSnapshot = toRecruitmentSnapshot(afterRecruitment);

    return new RecruitmentHistoryChangeLog(
        RECRUITMENT_SCOPE,
        beforeSnapshot,
        afterSnapshot,
        determineChangedFields(beforeSnapshot, afterSnapshot, Set.of()));
  }

  // 채용 단계 이력 변경 로그 생성
  public RecruitmentHistoryChangeLog createStageChangeLog(
      RecruitmentStage beforeStage, RecruitmentStage afterStage) {
    Map<String, Object> beforeSnapshot = toStageSnapshot(beforeStage);
    Map<String, Object> afterSnapshot = toStageSnapshot(afterStage);

    return new RecruitmentHistoryChangeLog(
        STAGE_SCOPE,
        beforeSnapshot,
        afterSnapshot,
        determineChangedFields(beforeSnapshot, afterSnapshot, Set.of("id")));
  }

  // 면접관 이력 변경 로그 생성
  public RecruitmentHistoryChangeLog createInterviewerChangeLog(
      Set<Long> beforeInterviewerIds, Set<Long> afterInterviewerIds) {
    Map<String, Object> beforeSnapshot = toInterviewerSnapshot(beforeInterviewerIds);
    Map<String, Object> afterSnapshot = toInterviewerSnapshot(afterInterviewerIds);

    return new RecruitmentHistoryChangeLog(
        INTERVIEWER_SCOPE,
        beforeSnapshot,
        afterSnapshot,
        determineChangedFields(beforeSnapshot, afterSnapshot, Set.of()));
  }

  // 채용 공고 이력 스냅샷 생성
  private Map<String, Object> toRecruitmentSnapshot(Recruitment recruitment) {
    if (recruitment == null) {
      return null;
    }

    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("title", recruitment.title());
    snapshot.put("contents", recruitment.contents());
    snapshot.put(
        "recruitmentStatus",
        recruitment.recruitmentStatus() != null ? recruitment.recruitmentStatus().name() : null);
    snapshot.put("targetCount", recruitment.targetCount());
    snapshot.put(
        "startDate",
        normalizeDateTime(recruitment.startDate()) != null
            ? normalizeDateTime(recruitment.startDate()).toString()
            : null);
    snapshot.put(
        "endDate",
        normalizeDateTime(recruitment.endDate()) != null
            ? normalizeDateTime(recruitment.endDate()).toString()
            : null);
    snapshot.put("applicationTemplateId", recruitment.applicationTemplateId());
    snapshot.put(
        "careerType", recruitment.careerType() != null ? recruitment.careerType().name() : null);
    snapshot.put("minExperienceYears", recruitment.minExperienceYears());
    snapshot.put("maxExperienceYears", recruitment.maxExperienceYears());
    snapshot.put("leadGroupId", recruitment.leadGroupId());
    snapshot.put(
        "referenceGroupIds",
        recruitment.referenceGroupIds() == null
            ? List.of()
            : List.copyOf(recruitment.referenceGroupIds()));
    return snapshot;
  }

  // 채용 단계 이력 스냅샷 생성
  private Map<String, Object> toStageSnapshot(RecruitmentStage stage) {
    if (stage == null) {
      return null;
    }

    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("id", stage.id());
    snapshot.put("stageName", stage.stageName());
    snapshot.put("stageStep", stage.stageStep());
    snapshot.put("stageType", stage.stageType() != null ? stage.stageType().name() : null);
    return snapshot;
  }

  // 면접관 이력 스냅샷 생성
  private Map<String, Object> toInterviewerSnapshot(Set<Long> interviewerIds) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put(
        "interviewerIds", interviewerIds == null ? List.of() : List.copyOf(interviewerIds));
    return snapshot;
  }

  // 변경 필드 목록 추출
  private List<String> determineChangedFields(
      Map<String, Object> beforeChangeLog,
      Map<String, Object> afterChangeLog,
      Set<String> ignoredFields) {
    Set<String> fieldNames = new LinkedHashSet<>();
    if (beforeChangeLog != null) {
      fieldNames.addAll(beforeChangeLog.keySet());
    }
    if (afterChangeLog != null) {
      fieldNames.addAll(afterChangeLog.keySet());
    }
    fieldNames.removeAll(ignoredFields);

    List<String> changedFields = new ArrayList<>();
    for (String fieldName : fieldNames) {
      Object beforeValue = beforeChangeLog != null ? beforeChangeLog.get(fieldName) : null;
      Object afterValue = afterChangeLog != null ? afterChangeLog.get(fieldName) : null;
      if (!Objects.equals(beforeValue, afterValue)) {
        changedFields.add(fieldName);
      }
    }
    return changedFields;
  }

  private LocalDateTime normalizeDateTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.truncatedTo(ChronoUnit.MILLIS);
  }
}
