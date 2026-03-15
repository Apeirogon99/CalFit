package com.coffiness.calfit.core.enums;

import lombok.Getter;

@Getter
public enum RecruitmentStatus {
  DRAFT("대기(임시저장)"),
  OPEN("진행 중"),
  CLOSED("닫힘(마감)");

  private final String description;

  RecruitmentStatus(String description) {
    this.description = description;
  }
}
