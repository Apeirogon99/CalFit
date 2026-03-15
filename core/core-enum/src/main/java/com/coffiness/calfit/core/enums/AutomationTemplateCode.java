package com.coffiness.calfit.core.enums;

import java.util.Arrays;
import java.util.List;

public enum AutomationTemplateCode {
  DOCUMENT_PASS("서류 합격 안내", AutomationActionType.EMAIL, List.of(RecruitmentStageType.DOCUMENT)),
  INTERVIEW_REQUEST(
      "면접 요청 안내", AutomationActionType.EMAIL, List.of(RecruitmentStageType.INTERVIEW)),
  FINAL_PASS(
      "최종 합격 안내",
      AutomationActionType.EMAIL,
      List.of(RecruitmentStageType.PASS, RecruitmentStageType.OFFER)),
  FAIL_NOTIFICATION("불합격 안내", AutomationActionType.EMAIL, List.of(RecruitmentStageType.FAIL));

  private final String label;
  private final AutomationActionType actionType;
  private final List<RecruitmentStageType> recommendedStageTypes;

  AutomationTemplateCode(
      String label,
      AutomationActionType actionType,
      List<RecruitmentStageType> recommendedStageTypes) {
    this.label = label;
    this.actionType = actionType;
    this.recommendedStageTypes = recommendedStageTypes;
  }

  public String getLabel() {
    return label;
  }

  public AutomationActionType getActionType() {
    return actionType;
  }

  public List<RecruitmentStageType> getRecommendedStageTypes() {
    return recommendedStageTypes;
  }

  public static boolean supports(String code) {
    return Arrays.stream(values()).anyMatch(value -> value.name().equals(code));
  }
}
