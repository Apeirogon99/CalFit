package com.coffiness.calfit.core.enums;

public enum EventStatus {
  CONFIRMED("confirmed", "확정됨"),
  TENTATIVE("tentative", "미정"),
  CANCELLED("cancelled", "취소됨");

  private final String googleApiValue;
  private final String description;

  EventStatus(String googleApiValue, String description) {
    this.googleApiValue = googleApiValue;
    this.description = description;
  }
}
