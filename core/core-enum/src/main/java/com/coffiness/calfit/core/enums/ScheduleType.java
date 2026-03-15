package com.coffiness.calfit.core.enums;

public enum ScheduleType {
  INTERVIEW("면접"),
  MEETING("회의"),
  VACATION("휴가"),
  BUSINESS("외근/출장"),
  OTHERS("기타");

  private final String description;

  private ScheduleType(final String description) {
    this.description = description;
  }
}
