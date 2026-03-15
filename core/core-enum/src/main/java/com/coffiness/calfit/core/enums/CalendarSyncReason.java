package com.coffiness.calfit.core.enums;

/*
 * 캘린더 동기화 판단 사유 enum
 * */
public enum CalendarSyncReason {
  APPLY_MISSING_GOOGLE_TIMESTAMP("구글 갱신 시각 없음"),
  APPLY_CALFIT_TIMESTAMP_MISSING("Calfit 갱신 시각 없음"),
  APPLY_GOOGLE_NEWER_THAN_CALFIT("구글 변경이 Calfit 보다 최신"),
  IGNORE_ECHO_FROM_CALFIT("Calfit 반사 동기화 무시"),
  IGNORE_GOOGLE_NOT_NEWER_THAN_CALFIT("구글 변경이 Calfit 보다 최신 아님");

  private final String description;

  CalendarSyncReason(String description) {
    this.description = description;
  }
}
