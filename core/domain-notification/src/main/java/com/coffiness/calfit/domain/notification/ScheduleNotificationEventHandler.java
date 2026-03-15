package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.domain.event.ScheduleAttendeeNotificationEvent;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor

/*
 * 일정 참석자 알림 핸들러
 * */
public class ScheduleNotificationEventHandler {

  private final NotificationService notificationService;
  private final NotificationSseService notificationSseService;
  private final NotificationTemplateFactory notificationTemplateFactory;

  // 일정 알림 저장 및 SSE 전송
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ScheduleAttendeeNotificationEvent event) {
    try {
      TenantContext.setTenantId(event.tenantId());

      NotificationMessage message =
          notificationTemplateFactory.buildScheduleAttendeeNotification(event);

      List<NotificationCreateCommand> commands =
          event.attendeeUserIds().stream()
              .filter(userId -> userId != null && !userId.equals(event.ownerUserId()))
              .distinct()
              .map(
                  userId ->
                      new NotificationCreateCommand(
                          userId,
                          message.type(),
                          message.title(),
                          message.content(),
                          message.targetType(),
                          message.targetId(),
                          message.actionUrl()))
              .toList();

      List<Notification> notifications = notificationService.createAll(commands);
      notifications.forEach(
          notification ->
              notificationSseService.sendNotificationCreated(
                  event.tenantId(),
                  notification.recipientUserId(),
                  notification.id(),
                  notification.type()));
    } catch (RuntimeException e) {
      log.error("일정 알림 생성 실패. scheduleId={}", event.scheduleId(), e);
    } finally {
      TenantContext.clear();
    }
  }
}
