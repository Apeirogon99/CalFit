package com.coffiness.calfit.domain.sync.policy;

import com.coffiness.calfit.core.enums.CalendarSyncReason;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 * 구글 -> Calfit 동기화 반영 여부를 판단하는 정책 클래스
 * */
@Component
@RequiredArgsConstructor
public class CalendarSyncPolicy {

  private static final Duration ECHO_GUARD_WINDOW = Duration.ofSeconds(90);
  private static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(5);
  private final ZoneId appZoneId;

  public CalendarSyncDecision decideGoogleToCalfit(
      LocalDateTime calfitUpdatedAt,
      LocalDateTime lastCalfitPushedAt,
      ZonedDateTime googleUpdatedAt) {

    // 구글 타임스탬프 누락 검사
    if (googleUpdatedAt == null) {
      return CalendarSyncDecision.apply(CalendarSyncReason.APPLY_MISSING_GOOGLE_TIMESTAMP);
    }

    // 에코 현상 방어
    LocalDateTime googleUpdatedAtLocal = toServerLocalTime(googleUpdatedAt);

    // 90초 이내 변경 알림 수신 시 무시
    if (isEchoFromCalfit(lastCalfitPushedAt, googleUpdatedAtLocal)) {
      return CalendarSyncDecision.ignore(CalendarSyncReason.IGNORE_ECHO_FROM_CALFIT);
    }

    // 로컬 데이터 부재 검사
    if (calfitUpdatedAt == null) {
      return CalendarSyncDecision.apply(CalendarSyncReason.APPLY_CALFIT_TIMESTAMP_MISSING);
    }

    // 로컬과 구글 데이터 타임 스탬프 비교
    LocalDateTime calfitWinsUntil = calfitUpdatedAt.plus(CLOCK_SKEW_TOLERANCE);
    if (!googleUpdatedAtLocal.isAfter(calfitWinsUntil)) {
      return CalendarSyncDecision.ignore(CalendarSyncReason.IGNORE_GOOGLE_NOT_NEWER_THAN_CALFIT);
    }

    return CalendarSyncDecision.apply(CalendarSyncReason.APPLY_GOOGLE_NEWER_THAN_CALFIT);
  }

  private LocalDateTime toServerLocalTime(ZonedDateTime googleUpdatedAt) {
    return googleUpdatedAt.withZoneSameInstant(appZoneId).toLocalDateTime();
  }

  private boolean isEchoFromCalfit(
      LocalDateTime lastCalfitPushedAt, LocalDateTime googleUpdatedAtLocal) {
    if (lastCalfitPushedAt == null) {
      return false;
    }

    LocalDateTime echoWindowEnd = lastCalfitPushedAt.plus(ECHO_GUARD_WINDOW);
    return !googleUpdatedAtLocal.isBefore(lastCalfitPushedAt)
        && !googleUpdatedAtLocal.isAfter(echoWindowEnd);
  }
}
