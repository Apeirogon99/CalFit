package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.notification.Notification;
import com.coffiness.calfit.domain.notification.NotificationCategory;
import com.coffiness.calfit.domain.notification.NotificationPage;
import com.coffiness.calfit.domain.notification.NotificationReader;
import com.coffiness.calfit.storage.db.core.notification.NotificationEntity;
import com.coffiness.calfit.storage.db.core.notification.NotificationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationReaderImpl implements NotificationReader {

  private final NotificationRepository notificationRepository;

  @Override
  public NotificationPage getRecentNotifications(
      String tenantId, Long recipientUserId, int page, int size) {
    org.springframework.data.domain.Page<NotificationEntity> notificationPage =
        notificationRepository.findByTenantIdAndRecipientUserIdAndStatusOrderByCreatedAtDesc(
            tenantId, recipientUserId, EntityStatus.ACTIVE, PageRequest.of(page, size));

    return new NotificationPage(
        notificationPage.getContent().stream().map(this::toNotification).toList(),
        notificationPage.hasNext());
  }

  @Override
  public NotificationPage getRecentNotifications(
      String tenantId, Long recipientUserId, NotificationCategory category, int page, int size) {
    org.springframework.data.domain.Page<NotificationEntity> notificationPage =
        notificationRepository
            .findByTenantIdAndRecipientUserIdAndStatusAndTypeInOrderByCreatedAtDesc(
                tenantId,
                recipientUserId,
                EntityStatus.ACTIVE,
                List.copyOf(category.types()),
                PageRequest.of(page, size));

    return new NotificationPage(
        notificationPage.getContent().stream().map(this::toNotification).toList(),
        notificationPage.hasNext());
  }

  @Override
  public NotificationPage getUnreadNotifications(
      String tenantId, Long recipientUserId, int page, int size) {
    org.springframework.data.domain.Page<NotificationEntity> notificationPage =
        notificationRepository
            .findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseOrderByCreatedAtDesc(
                tenantId, recipientUserId, EntityStatus.ACTIVE, PageRequest.of(page, size));

    return new NotificationPage(
        notificationPage.getContent().stream().map(this::toNotification).toList(),
        notificationPage.hasNext());
  }

  @Override
  public NotificationPage getUnreadNotifications(
      String tenantId, Long recipientUserId, NotificationCategory category, int page, int size) {
    org.springframework.data.domain.Page<NotificationEntity> notificationPage =
        notificationRepository
            .findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseAndTypeInOrderByCreatedAtDesc(
                tenantId,
                recipientUserId,
                EntityStatus.ACTIVE,
                List.copyOf(category.types()),
                PageRequest.of(page, size));

    return new NotificationPage(
        notificationPage.getContent().stream().map(this::toNotification).toList(),
        notificationPage.hasNext());
  }

  @Override
  public Optional<Notification> getNotification(
      String tenantId, Long recipientUserId, Long notificationId) {
    return notificationRepository
        .findByTenantIdAndRecipientUserIdAndIdAndStatus(
            tenantId, recipientUserId, notificationId, EntityStatus.ACTIVE)
        .map(this::toNotification);
  }

  @Override
  public List<Notification> getUnreadNotifications(String tenantId, Long recipientUserId) {
    return notificationRepository
        .findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseOrderByCreatedAtDesc(
            tenantId, recipientUserId, EntityStatus.ACTIVE)
        .stream()
        .map(this::toNotification)
        .toList();
  }

  @Override
  public long countUnreadNotifications(String tenantId, Long recipientUserId) {
    return notificationRepository.countByTenantIdAndRecipientUserIdAndStatusAndIsReadFalse(
        tenantId, recipientUserId, EntityStatus.ACTIVE);
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
