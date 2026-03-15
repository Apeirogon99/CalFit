package com.coffiness.calfit.domain.sync.policy;

import com.coffiness.calfit.core.enums.CalendarSyncReason;

/*
 * 캘린더 동기화 정책의 판단 결과 모델
 * */
public record CalendarSyncDecision(boolean apply, CalendarSyncReason reason) {

  public static CalendarSyncDecision apply(CalendarSyncReason reason) {
    return new CalendarSyncDecision(true, reason);
  }

  public static CalendarSyncDecision ignore(CalendarSyncReason reason) {
    return new CalendarSyncDecision(false, reason);
  }
}
