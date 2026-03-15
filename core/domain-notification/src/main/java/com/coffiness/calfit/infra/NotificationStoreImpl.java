package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.notification.Notification;
import com.coffiness.calfit.domain.notification.NotificationCreateCommand;
import com.coffiness.calfit.domain.notification.NotificationStore;
import com.coffiness.calfit.storage.db.core.notification.NotificationEntity;
import com.coffiness.calfit.storage.db.core.notification.NotificationRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationStoreImpl implements NotificationStore {

  private final NotificationRepository notificationRepository;

  @Override
  public Notification create(String tenantId, NotificationCreateCommand command) {
    NotificationEntity saved = notificationRepository.save(toEntity(tenantId, command));
    return toNotification(saved);
  }

  @Override
  public List<Notification> createAll(String tenantId, List<NotificationCreateCommand> commands) {
    if (commands.isEmpty()) {
      return List.of();
    }

    return notificationRepository
        .saveAll(commands.stream().map(command -> toEntity(tenantId, command)).toList())
        .stream()
        .map(this::toNotification)
        .toList();
  }

  @Override
  public Notification markAsRead(String tenantId, Long recipientUserId, Long notificationId) {
    NotificationEntity entity =
        notificationRepository
            .findByTenantIdAndRecipientUserIdAndIdAndStatus(
                tenantId, recipientUserId, notificationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.markAsRead(LocalDateTime.now());
    return toNotification(entity);
  }

  @Override
  public void markAllAsRead(String tenantId, Long recipientUserId) {
    notificationRepository
        .findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseOrderByCreatedAtDesc(
            tenantId, recipientUserId, EntityStatus.ACTIVE)
        .forEach(entity -> entity.markAsRead(LocalDateTime.now()));
  }

  @Override
  public void delete(String tenantId, Long recipientUserId, Long notificationId) {
    NotificationEntity entity =
        notificationRepository
            .findByTenantIdAndRecipientUserIdAndIdAndStatus(
                tenantId, recipientUserId, notificationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.deleted();
  }

  @Override
  public void deleteAll(String tenantId, Long recipientUserId) {
    notificationRepository.updateStatusByTenantIdAndRecipientUserIdAndStatus(
        tenantId, recipientUserId, EntityStatus.ACTIVE, EntityStatus.DELETED);
  }

  private NotificationEntity toEntity(String tenantId, NotificationCreateCommand command) {
    return NotificationEntity.builder()
        .tenantId(tenantId)
        .recipientUserId(command.recipientUserId())
        .type(command.type())
        .title(command.title())
        .content(command.content())
        .targetType(command.targetType())
        .targetId(command.targetId())
        .actionUrl(command.actionUrl())
        .isRead(false)
        .readAt(null)
        .build();
  }

  private Notification toNotification(NotificationEntity entity) {
    return new Notification(
        entity.getId(),
        entity.getRecipientUserId(),
        entity.getType(),
        entity.getTitle(),
        entity.getContent(),
        entity.getTargetType(),
        entity.getTargetId(),
        entity.getActionUrl(),
        entity.isRead(),
        entity.getReadAt(),
        entity.getCreatedAt());
  }
}
