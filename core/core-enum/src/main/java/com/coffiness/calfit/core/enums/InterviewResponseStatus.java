package com.coffiness.calfit.core.enums;

public enum InterviewResponseStatus {
  ACCEPTED("수락"),
  DECLINED("거절");

  InterviewResponseStatus(String description) {
    this.description = description;
  }

  private final String description;
}
