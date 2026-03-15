package com.coffiness.calfit.core.enums;

import lombok.Getter;

@Getter
public enum RecruitmentStageType {
  DOCUMENT("서류 단계"),
  INTERVIEW("면접 단계"),
  TEST("시험 단계"),
  OFFER("처우 협의"),
  PASS("최종 합격"),
  FAIL("불합격");

  private final String description;

  RecruitmentStageType(String description) {
    this.description = description;
  }
}
