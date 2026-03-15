package com.coffiness.calfit.core.enums;

public enum InterviewEventType {
  CREATED("면접 일정 생성"),
  RESCHEDULED("일정 변경"),
  STATUS_CHANGED("상태 변경"),
  INTERVIEWER_ASSIGNED("면접관 배정"),
  INTERVIEWER_REMOVED("면접관 해제"),
  INTERVIEWER_ACCEPTED("면접관 수락"),
  INTERVIEWER_DECLINED("면접관 거절"),
  APPLICANT_ASSIGNED("지원자 배정"),
  APPLICANT_REMOVED("지원자 해제");

  InterviewEventType(String description) {
    this.description = description;
  }

  private final String description;
}
