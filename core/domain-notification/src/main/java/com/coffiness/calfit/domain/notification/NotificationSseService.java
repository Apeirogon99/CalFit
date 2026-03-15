package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationSseService {
  private static final long DEFAULT_TIMEOUT_MILLIS = 30L * 60L * 1000L;
  private static final long HEARTBEAT_INTERVAL_MILLIS = 10L * 1000L;

  private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(String tenantId, Long userId) {
    SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
    String key = key(tenantId, userId);

    emitters.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

    emitter.onCompletion(() -> remove(key, emitter));
    emitter.onTimeout(() -> remove(key, emitter));
    emitter.onError(ex -> remove(key, emitter));

    send(key, emitter, "connected", Map.of("event", "connected"));
    return emitter;
  }

  public void sendNotificationCreated(
      String tenantId, Long userId, Long notificationId, NotificationType type) {
    String key = key(tenantId, userId);
    List<SseEmitter> userEmitters = emitters.get(key);
    if (userEmitters == null || userEmitters.isEmpty()) {
      return;
    }

    Map<String, Object> payload =
        Map.of(
            "event", "notification-created", "notificationId", notificationId, "type", type.name());

    for (SseEmitter emitter : userEmitters) {
      send(key, emitter, "notification-created", payload);
    }
  }

  // 같은 워크스페이스에 접속 중인 사용자들에게 실시간 이벤트를 전파
  public void sendWorkspaceEvent(String tenantId, String eventName, Object payload) {
    String tenantPrefix = tenantId + ":";

    emitters.forEach(
        (key, userEmitters) -> {
          if (!key.startsWith(tenantPrefix)) {
            return;
          }

          for (SseEmitter emitter : userEmitters) {
            send(key, emitter, eventName, payload);
          }
        });
  }

  @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MILLIS)
  public void sendHeartbeat() {
    emitters.forEach(
        (key, userEmitters) -> {
          for (SseEmitter emitter : userEmitters) {
            send(key, emitter, "heartbeat", Map.of("event", "heartbeat"));
          }
        });
  }

  private void send(String key, SseEmitter emitter, String eventName, Object payload) {
    try {
      emitter.send(SseEmitter.event().name(eventName).data(payload));
    } catch (IOException | IllegalStateException exception) {
      remove(key, emitter);
    }
  }

  private void remove(String key, SseEmitter emitter) {
    CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(key);
    if (userEmitters == null) {
      return;
    }
    userEmitters.remove(emitter);
    if (userEmitters.isEmpty()) {
      emitters.remove(key);
    }
  }

  private String key(String tenantId, Long userId) {
    return tenantId + ":" + userId;
  }
}
