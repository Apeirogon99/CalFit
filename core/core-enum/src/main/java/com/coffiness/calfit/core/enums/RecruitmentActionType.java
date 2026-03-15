package com.coffiness.calfit.core.enums;

import lombok.Getter;

@Getter
public enum RecruitmentActionType {
  STAGE_CREATED("단계 생성"),
  STAGE_UPDATED("단계 설정 수정"),
  STAGE_DELETED("단계 삭제"),
  STAGE_REORDERED("단계 순서 변경"),

  INTERVIEWER_ADDED("면접관 배정"),
  INTERVIEWER_REMOVED("면접관 해제"),

  RECRUITMENT_CREATED("공고 최초 생성"),
  RECRUITMENT_OPENED("공고 게시"),
  RECRUITMENT_CLOSED("공고 마감"),
  RECRUITMENT_INFO_UPDATED("공고 기본정보 수정"),
  RECRUITMENT_DELETE("공고 삭제");

  private final String description;

  RecruitmentActionType(String description) {
    this.description = description;
  }
}
