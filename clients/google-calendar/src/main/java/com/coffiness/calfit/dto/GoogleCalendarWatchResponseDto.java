package com.coffiness.calfit.dto;

import com.coffiness.calfit.model.GoogleCalendarWatchResult;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/*
 * 구글 Calendar API watch 채널 생성 응답 DTO
 * */
public record GoogleCalendarWatchResponseDto(String id, String resourceId, String expiration) {

  // 클라이언트 응답 DTO를 도메인 결과 모델로 변환
  public GoogleCalendarWatchResult toResult() {
    return new GoogleCalendarWatchResult(id, resourceId, parseExpiration(expiration));
  }

  // 구글 expiration을 UTC LocalDateTime으로 변환
  private LocalDateTime parseExpiration(String expirationMillis) {
    if (expirationMillis == null || expirationMillis.isBlank()) {
      return null;
    }

    try {
      long millis = Long.parseLong(expirationMillis);
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
