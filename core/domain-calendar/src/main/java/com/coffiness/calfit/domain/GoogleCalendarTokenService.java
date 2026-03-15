package com.coffiness.calfit.domain;

import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.model.OAuthRefreshResult;
import com.coffiness.calfit.port.GoogleOAuthPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 구글 캘린더 토큰의 만료 판단과 갱신을 담당
 * */
@Service
@RequiredArgsConstructor
@Transactional
public class GoogleCalendarTokenService {

  private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 120L;

  private final ExternalCalendarReader externalCalendarReader;
  private final ExternalCalendarStore externalCalendarStore;
  private final ScheduleStore scheduleStore;
  private final GoogleOAuthPort googleOAuthPort;
  private final Clock clock;

  // 구글 연결 직후 토큰을 저장하거나 기존 연결을 워크스페이스 캘린더 기준으로 갱신
  public ExternalCalendar upsertConnectedToken(
      Long userId, String calendarId, OAuthExchangeResult result) {
    validateExchangeResult(result);

    LocalDateTime expiresAt = calculateExpiresAt(result.expiresIn());

    ExternalCalendar existingByCalendarId =
        externalCalendarReader.readByUserIdAndCalendarId(userId, calendarId);
    if (existingByCalendarId != null) {
      String refreshToken =
          resolveRefreshToken(result.refreshToken(), existingByCalendarId.refreshToken());
      return externalCalendarStore.updateConnectedCalendar(
          existingByCalendarId.id(), calendarId, result.accessToken(), refreshToken, expiresAt);
    }

    ExternalCalendar existing = externalCalendarReader.readSyncEnabledByUserId(userId);
    if (existing != null) {
      String refreshToken = resolveRefreshToken(result.refreshToken(), existing.refreshToken());
      boolean calendarChanged = isCalendarChanged(existing.calendarId(), calendarId);

      ExternalCalendar updated =
          externalCalendarStore.updateConnectedCalendar(
              existing.id(), calendarId, result.accessToken(), refreshToken, expiresAt);

      if (calendarChanged) {
        scheduleStore.clearGoogleEventIdsByUserId(userId);
      }

      return updated;
    }

    if (isBlank(result.refreshToken())) {
      throw new IllegalStateException("GOOGLE_REAUTH_REQUIRED");
    }

    ExternalCalendar toCreate =
        new ExternalCalendar(
            null,
            userId,
            calendarId,
            result.accessToken(),
            result.refreshToken(),
            expiresAt,
            null,
            true,
            null,
            null,
            null);

    return externalCalendarStore.store(toCreate);
  }

  // 현재 사용 가능한 access token 을 반환하고, 만료 임박 시 refresh 후 갱신
  public String getValidAccessToken(Long externalCalendarId) {
    ExternalCalendar snapshot = externalCalendarReader.read(externalCalendarId);

    if (!isExpiringSoon(snapshot.tokenExpiresAt())) {
      return snapshot.accessToken();
    }

    ExternalCalendar locked = externalCalendarReader.readForUpdate(externalCalendarId);
    if (!isExpiringSoon(locked.tokenExpiresAt())) {
      return locked.accessToken();
    }

    if (isBlank(locked.refreshToken())) {
      throw new IllegalStateException("GOOGLE_REAUTH_REQUIRED");
    }

    OAuthRefreshResult refreshed;
    try {
      refreshed = googleOAuthPort.refreshAccessToken(locked.refreshToken());
    } catch (IllegalStateException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IllegalStateException("GOOGLE_OAUTH_ERROR", e);
    }

    validateRefreshResult(refreshed);

    LocalDateTime expiresAt = calculateExpiresAt(refreshed.expiresIn());
    String nextRefreshToken = resolveRefreshToken(refreshed.refreshToken(), locked.refreshToken());

    ExternalCalendar updated =
        externalCalendarStore.updateAuthTokens(
            locked.id(), refreshed.accessToken(), nextRefreshToken, expiresAt);

    return updated.accessToken();
  }

  // OAuth expiresIn 값을 기준으로 토큰 만료 시각을 계산
  private LocalDateTime calculateExpiresAt(Long expiresIn) {
    if (expiresIn == null || expiresIn <= 0) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }

    return LocalDateTime.now(clock).plusSeconds(expiresIn);
  }

  // 토큰 만료 시각이 없거나, 현재 시각 기준 버퍼 이내로 만료 예정이면 true 를 반환
  private boolean isExpiringSoon(LocalDateTime tokenExpiresAt) {
    if (tokenExpiresAt == null) {
      return true;
    }

    LocalDateTime now = LocalDateTime.now(clock);
    return !tokenExpiresAt.isAfter(now.plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
  }

  // refresh token을 새 값이 오면 교체하고, 없으면 기존 값을 유지
  private String resolveRefreshToken(String newRefreshToken, String existingRefreshToken) {
    return !isBlank(newRefreshToken) ? newRefreshToken : existingRefreshToken;
  }

  // authorization code 교환 응답의 유효성을 검증
  private void validateExchangeResult(OAuthExchangeResult result) {
    if (result == null || isBlank(result.accessToken())) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }
    if (isBlank(result.idToken())) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }
  }

  // refresh 응답의 유효성을 검증
  private void validateRefreshResult(OAuthRefreshResult result) {
    if (result == null || isBlank(result.accessToken())) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }
    if (result.expiresIn() == null || result.expiresIn() <= 0) {
      throw new IllegalStateException("GOOGLE_OAUTH_INVALID_RESPONSE");
    }
  }

  // 문자열이 null 이거나 공백인지 확인
  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private boolean isCalendarChanged(String previousCalendarId, String nextCalendarId) {
    return !Objects.equals(previousCalendarId, nextCalendarId);
  }
}
