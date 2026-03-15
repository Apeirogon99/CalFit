package com.coffiness.calfit.core.enums;

public enum InterviewStatus {
  PENDING("대기"),
  CONFIRMED("확정"),
  CANCELLED("취소"),
  COMPLETED("완료");

  InterviewStatus(String description) {
    this.description = description;
  }

  private final String description;
}
