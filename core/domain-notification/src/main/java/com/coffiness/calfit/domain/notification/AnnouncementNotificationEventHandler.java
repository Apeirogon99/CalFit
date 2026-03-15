package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.domain.announcementBoard.event.AnnouncementCreatedEvent;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
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
public class AnnouncementNotificationEventHandler {

  private final NotificationService notificationService;
  private final NotificationSseService notificationSseService;
  private final NotificationTemplateFactory notificationTemplateFactory;
  private final MemberReader memberReader;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(AnnouncementCreatedEvent event) {
    try {
      TenantContext.setTenantId(event.tenantId());

      NotificationMessage message = notificationTemplateFactory.buildAnnouncementCreated(event);

      List<NotificationCreateCommand> commands =
          memberReader.getMembers().stream()
              .filter(member -> event.tenantId().equals(member.workspaceId()))
              .map(
                  member ->
                      new NotificationCreateCommand(
                          member.userId(),
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
      log.error(
          "Failed to create announcement notifications. announcementId={}",
          event.announcementId(),
          e);
    } finally {
      TenantContext.clear();
    }
  }
}
