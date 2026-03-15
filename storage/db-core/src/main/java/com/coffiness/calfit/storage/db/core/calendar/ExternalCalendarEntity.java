package com.coffiness.calfit.storage.db.core.calendar;

import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 구글 계정 연동 엔티티
 * */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "external_calendars",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_external_calendars_user_calendar",
          columnNames = {"tenant_id", "user_id", "calendar_id"})
    })
public class ExternalCalendarEntity extends TenantBaseEntity {

  // 연관 사용자의 식별자
  @Column(name = "user_id", nullable = false)
  private Long userId;

  // 구글 캘린더 ID (보통 구글 이메일 주소)
  @Column(name = "calendar_id", nullable = false)
  private String calendarId;

  // OAuth2 인증 정보
  // TODO: 추후 암호화 적용 예정
  @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
  private String accessToken;

  @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
  private String refreshToken;

  @Column(name = "token_expires_at", nullable = false)
  private LocalDateTime tokenExpiresAt;

  // 변경된 일정만 가져오는 동기화한 지점의 토큰
  @Column(name = "sync_token", columnDefinition = "TEXT")
  private String syncToken;

  @Column(name = "is_sync_enabled", nullable = false)
  private boolean isSyncEnabled;

  // 구글 watch 채널 ID
  @Column(name = "channel_id")
  private String channelId;

  // 구글 watch 리소스 ID
  @Column(name = "channel_resource_id")
  private String channelResourceId;

  // 구글 watch 만료 시각(UTC)
  @Column(name = "channel_expires_at")
  private LocalDateTime channelExpiresAt;

  @Builder
  public ExternalCalendarEntity(
      String tenantId,
      Long userId,
      String calendarId,
      String accessToken,
      String refreshToken,
      LocalDateTime tokenExpiresAt,
      String syncToken,
      boolean isSyncEnabled,
      String channelId,
      String channelResourceId,
      LocalDateTime channelExpiresAt) {
    super(tenantId);
    this.userId = userId;
    this.calendarId = calendarId;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenExpiresAt = tokenExpiresAt;
    this.syncToken = syncToken;
    this.isSyncEnabled = isSyncEnabled;
    this.channelId = channelId;
    this.channelResourceId = channelResourceId;
    this.channelExpiresAt = channelExpiresAt;
  }

  // 캘린더 연결 정보와 인증 토큰을 함께 갱신
  public void updateConnectedCalendar(
      String calendarId, String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {
    boolean calendarChanged = !Objects.equals(this.calendarId, calendarId);

    this.isSyncEnabled = true;
    this.calendarId = calendarId;
    this.accessToken = accessToken;
    this.tokenExpiresAt = tokenExpiresAt;

    // null 덮어쓰기 방지
    if (refreshToken != null) {
      this.refreshToken = refreshToken;
    }

    if (calendarChanged) {
      this.syncToken = null;
      this.channelId = null;
      this.channelResourceId = null;
      this.channelExpiresAt = null;
    }
  }

  // 토큰 갱신 메소드
  public void updateAuthTokens(
      String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {
    this.accessToken = accessToken;
    this.tokenExpiresAt = tokenExpiresAt;
    // null 덮어쓰기 방지
    if (refreshToken != null) {
      this.refreshToken = refreshToken;
    }
  }

  // 새로운 동기화 지점을 저장하는 메소드
  public void updateSyncToken(String syncToken) {
    this.syncToken = syncToken;
  }

  // watch 채널 정보를 저장하는 메소드
  public void updateWatchChannel(
      String channelId, String channelResourceId, LocalDateTime channelExpiresAt) {
    this.channelId = channelId;
    this.channelResourceId = channelResourceId;
    this.channelExpiresAt = channelExpiresAt;
  }
}
