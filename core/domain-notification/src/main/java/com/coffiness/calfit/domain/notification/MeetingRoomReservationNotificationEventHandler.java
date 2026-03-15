package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservationCreatedEvent;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRoomReservationNotificationEventHandler {

  private final NotificationService notificationService;
  private final NotificationSseService notificationSseService;
  private final NotificationTemplateFactory notificationTemplateFactory;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(MeetingRoomReservationCreatedEvent event) {
    try {
      TenantContext.setTenantId(event.tenantId());

      NotificationMessage message =
          notificationTemplateFactory.buildMeetingRoomReservationCreated(event);
      List<NotificationCreateCommand> commands =
          Stream.concat(Stream.of(event.actorUserId()), event.participantUserIds().stream())
              .filter(userId -> userId != null)
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

      if (commands.isEmpty()) {
        return;
      }

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
          "Failed to create meeting room reservation notifications. reservationId={}",
          event.reservationId(),
          e);
    } finally {
      TenantContext.clear();
    }
  }
}
