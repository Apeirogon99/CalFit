package com.coffiness.calfit.core.api.facade.calendar;

import com.coffiness.calfit.domain.CalendarConnectService;
import com.coffiness.calfit.domain.ExternalCalendar;
import com.coffiness.calfit.domain.ExternalCalendarReader;
import com.coffiness.calfit.domain.GoogleChannelTokenService;
import com.coffiness.calfit.domain.GoogleChannelTokenService.ChannelTokenPayload;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * 구글 캘린더 연동 요청의 워크스페이스 멤버 검증과 도메인 서비스 호출을 담당
 * */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarConnectFacade {

  private final MemberReader memberReader;
  private final WorkspaceReader workspaceReader;
  private final CalendarConnectService calendarConnectService;
  private final ExternalCalendarReader externalCalendarReader;
  private final GoogleChannelTokenService googleChannelTokenService;

  @Transactional
  public String connectGoogleCalendar(long userId, String authCode, String redirectUri) {
    Member member = validateAndGetMember(userId);
    String workspaceName = resolveWorkspaceName(member.workspaceId());

    return calendarConnectService.connectGoogleCalendar(
        authCode, redirectUri, userId, member.workspaceId(), workspaceName);
  }

  @Transactional
  public void syncGoogleCalendar(long userId) {
    Member member = validateAndGetMember(userId);

    calendarConnectService.ensureWatchChannel(userId, member.workspaceId());
    calendarConnectService.syncGoogleCalendar(userId);
  }

  // 구글 웹훅 헤더를 검증하고 해당 테넌트와 멤버 기준으로 즉시 동기화를 수행
  @Transactional
  public void handleGoogleCalendarNotification(
      String channelId, String resourceId, String channelToken, String resourceState) {
    if (!hasText(channelId) || !hasText(resourceId) || !hasText(channelToken)) {
      return;
    }

    if (!isSupportedResourceState(resourceState)) {
      return;
    }

    ChannelTokenPayload tokenPayload;
    try {
      tokenPayload = googleChannelTokenService.parseToken(channelToken);
    } catch (IllegalArgumentException e) {
      log.warn("잘못된 구글 채널 토큰입니다. channelId={}, resourceId={}", channelId, resourceId);
      return;
    }

    try {
      TenantContext.setTenantId(tokenPayload.tenantId());

      ExternalCalendar externalCalendar =
          externalCalendarReader.read(tokenPayload.externalCalendarId());
      if (!isChannelMatched(externalCalendar, channelId, resourceId)) {
        log.warn(
            "채널 정보가 일치하지 않습니다. tenantId={}, externalCalendarId={}, channelId={}, resourceId={}",
            tokenPayload.tenantId(),
            tokenPayload.externalCalendarId(),
            channelId,
            resourceId);
        return;
      }

      Member member = memberReader.getMember(tokenPayload.tenantId(), externalCalendar.userId());
      if (member == null) {
        log.warn(
            "웹훅 대상 멤버를 찾을 수 없습니다. tenantId={}, userId={}",
            tokenPayload.tenantId(),
            externalCalendar.userId());
        return;
      }

      calendarConnectService.syncGoogleCalendarByExternalCalendarId(
          externalCalendar.id(), member.userId());
    } catch (IllegalArgumentException e) {
      log.warn(
          "웹훅 동기화를 건너뜁니다. tenantId={}, externalCalendarId={}",
          tokenPayload.tenantId(),
          tokenPayload.externalCalendarId(),
          e);
    } finally {
      TenantContext.clear();
    }
  }

  // workspaceId 기준으로 표시용 워크스페이스 이름을 조회
  private String resolveWorkspaceName(String workspaceId) {
    Workspace workspace = workspaceReader.getWorkspace(workspaceId);
    if (workspace == null || !hasText(workspace.name())) {
      return workspaceId;
    }

    return workspace.name();
  }

  private Member validateAndGetMember(long userId) {
    String currentWorkspaceId = TenantContext.getTenantId();
    Member member = memberReader.getMember(currentWorkspaceId, userId);

    if (member == null) {
      throw new IllegalArgumentException("워크스페이스 멤버가 아닙니다.");
    }

    return member;
  }

  // 수신된 채널 정보가 저장된 watch 채널과 일치하는지 검증
  private boolean isChannelMatched(
      ExternalCalendar externalCalendar, String channelId, String resourceId) {
    return hasText(externalCalendar.channelId())
        && hasText(externalCalendar.channelResourceId())
        && channelId.equals(externalCalendar.channelId())
        && resourceId.equals(externalCalendar.channelResourceId());
  }

  // 구글 알림 상태 중 동기화 대상 상태인지 확인
  private boolean isSupportedResourceState(String resourceState) {
    return "sync".equalsIgnoreCase(resourceState)
        || "exists".equalsIgnoreCase(resourceState)
        || "not_exists".equalsIgnoreCase(resourceState);
  }

  // 문자열이 null이나 공백이 아닌지 확인
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
