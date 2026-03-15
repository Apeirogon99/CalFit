package com.coffiness.calfit.core.api.facade.notification;

import com.coffiness.calfit.domain.notification.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class NotificationSseFacade {

  private final NotificationSseService notificationSseService;

  public SseEmitter subscribe(String tenantId, Long userId) {
    return notificationSseService.subscribe(tenantId, userId);
  }

  // 같은 워크스페이스의 구독자들에게 실시간 이벤트를 전송
  public void sendWorkspaceEvent(String tenantId, String eventName, Object payload) {
    notificationSseService.sendWorkspaceEvent(tenantId, eventName, payload);
  }
}
