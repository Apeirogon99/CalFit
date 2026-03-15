package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.domain.notification.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String content,
    NotificationTargetType targetType,
    Long targetId,
    String actionUrl,
    boolean isRead,
    LocalDateTime readAt,
    LocalDateTime createdAt) {

  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.id(),
        notification.type(),
        notification.title(),
        notification.content(),
        notification.targetType(),
        notification.targetId(),
        notification.actionUrl(),
        notification.isRead(),
        notification.readAt(),
        notification.createdAt());
  }
}
