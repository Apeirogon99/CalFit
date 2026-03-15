package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.ExternalCalendar;
import com.coffiness.calfit.domain.ExternalCalendarStore;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarEntity;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalCalendarStoreImpl implements ExternalCalendarStore {

  private final ExternalCalendarRepository externalCalendarRepository;

  @Override
  public ExternalCalendar store(ExternalCalendar externalCalendar) {
    ExternalCalendarEntity entity =
        ExternalCalendarEntity.builder()
            .userId(externalCalendar.userId())
            .calendarId(externalCalendar.calendarId())
            .accessToken(externalCalendar.accessToken())
            .refreshToken(externalCalendar.refreshToken())
            .tokenExpiresAt(externalCalendar.tokenExpiresAt())
            .syncToken(externalCalendar.syncToken())
            .isSyncEnabled(externalCalendar.isSyncEnabled())
            .channelId(externalCalendar.channelId())
            .channelResourceId(externalCalendar.channelResourceId())
            .channelExpiresAt(externalCalendar.channelExpiresAt())
            .build();

    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);
    return toDomain(saved);
  }

  // 워크스페이스 캘린더 ID 및 인증 토큰을 함께 갱신
  @Override
  public ExternalCalendar updateConnectedCalendar(
      Long externalCalendarId,
      String calendarId,
      String accessToken,
      String refreshToken,
      LocalDateTime tokenExpiresAt) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatus(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    entity.updateConnectedCalendar(calendarId, accessToken, refreshToken, tokenExpiresAt);
    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);

    return toDomain(saved);
  }

  @Override
  public ExternalCalendar updateAuthTokens(
      Long externalCalendarId,
      String accessToken,
      String refreshToken,
      LocalDateTime tokenExpiresAt) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatus(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    entity.updateAuthTokens(accessToken, refreshToken, tokenExpiresAt);
    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);

    return toDomain(saved);
  }

  @Override
  public ExternalCalendar updateSyncToken(Long externalCalendarId, String syncToken) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatus(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    entity.updateSyncToken(syncToken);
    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);

    return toDomain(saved);
  }

  // 저장된 외부 캘린더 엔티티의 watch 채널 메타데이터를 업데이트
  @Override
  public ExternalCalendar updateWatchChannel(
      Long externalCalendarId,
      String channelId,
      String channelResourceId,
      LocalDateTime channelExpiresAt) {
    ExternalCalendarEntity entity =
        externalCalendarRepository
            .findByIdAndStatus(externalCalendarId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("구글 캘린더 연동 정보를 찾을 수 없습니다."));

    entity.updateWatchChannel(channelId, channelResourceId, channelExpiresAt);
    ExternalCalendarEntity saved = externalCalendarRepository.save(entity);

    return toDomain(saved);
  }

  private ExternalCalendar toDomain(ExternalCalendarEntity entity) {
    return new ExternalCalendar(
        entity.getId(),
        entity.getUserId(),
        entity.getCalendarId(),
        entity.getAccessToken(),
        entity.getRefreshToken(),
        entity.getTokenExpiresAt(),
        entity.getSyncToken(),
        entity.isSyncEnabled(),
        entity.getChannelId(),
        entity.getChannelResourceId(),
        entity.getChannelExpiresAt());
  }
}
