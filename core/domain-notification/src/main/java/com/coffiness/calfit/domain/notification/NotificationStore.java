package com.coffiness.calfit.domain.notification;

import java.util.List;

public interface NotificationStore {

  Notification create(String tenantId, NotificationCreateCommand command);

  List<Notification> createAll(String tenantId, List<NotificationCreateCommand> commands);

  Notification markAsRead(String tenantId, Long recipientUserId, Long notificationId);

  void markAllAsRead(String tenantId, Long recipientUserId);

  void delete(String tenantId, Long recipientUserId, Long notificationId);

  void deleteAll(String tenantId, Long recipientUserId);
}
