package com.coffiness.calfit.core.api.facade.dashboard;

import com.coffiness.calfit.api.v1.response.DashboardChatMessageResponse;
import com.coffiness.calfit.core.api.facade.notification.NotificationSseFacade;
import com.coffiness.calfit.domain.notification.DashboardChatService;
import com.coffiness.calfit.domain.notification.DashboardChatService.DashboardChatMessage;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * 대시보드 익명 채팅의 조회와 실시간 전파를 조합
 * */
@Component
@RequiredArgsConstructor
public class DashboardChatFacade {

  private static final String CHAT_EVENT_NAME = "workspace-chat-message-created";

  private final DashboardChatService dashboardChatService;
  private final NotificationSseFacade notificationSseFacade;

  // 워크스페이스의 최근 익명 채팅 메시지를 조회
  @Transactional(readOnly = true)
  public List<DashboardChatMessageResponse> getRecentMessages(String tenantId, Long userId) {
    return dashboardChatService.getRecentMessages(tenantId).stream()
        .map(message -> toResponse(message, userId))
        .toList();
  }

  // 새 채팅 메시지를 저장하고 같은 워크스페이스 구독자에게 실시간 전파
  @Transactional
  public DashboardChatMessageResponse createMessage(String tenantId, Long userId, String content) {
    DashboardChatMessage message = dashboardChatService.createMessage(tenantId, userId, content);

    notificationSseFacade.sendWorkspaceEvent(
        tenantId,
        CHAT_EVENT_NAME,
        Map.of("event", CHAT_EVENT_NAME, "message", toResponse(message, null)));

    return toResponse(message, userId);
  }

  // 내부 메시지를 화면 응답 포맷으로 변환
  private DashboardChatMessageResponse toResponse(
      DashboardChatMessage message, Long currentUserId) {
    boolean mine = currentUserId != null && currentUserId.equals(message.userId());
    return new DashboardChatMessageResponse(
        message.id(), message.alias(), message.content(), message.createdAt(), mine);
  }
}
