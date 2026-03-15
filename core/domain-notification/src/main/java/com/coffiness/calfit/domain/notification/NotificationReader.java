package com.coffiness.calfit.domain.notification;

import java.util.List;
import java.util.Optional;

public interface NotificationReader {

  NotificationPage getRecentNotifications(
      String tenantId, Long recipientUserId, int page, int size);

  NotificationPage getRecentNotifications(
      String tenantId, Long recipientUserId, NotificationCategory category, int page, int size);

  NotificationPage getUnreadNotifications(
      String tenantId, Long recipientUserId, int page, int size);

  NotificationPage getUnreadNotifications(
      String tenantId, Long recipientUserId, NotificationCategory category, int page, int size);

  Optional<Notification> getNotification(
      String tenantId, Long recipientUserId, Long notificationId);

  List<Notification> getUnreadNotifications(String tenantId, Long recipientUserId);

  long countUnreadNotifications(String tenantId, Long recipientUserId);
}
