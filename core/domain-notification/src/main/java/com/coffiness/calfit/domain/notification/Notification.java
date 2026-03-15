package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import java.time.LocalDateTime;

public record Notification(
    Long id,
    Long recipientUserId,
    NotificationType type,
    String title,
    String content,
    NotificationTargetType targetType,
    Long targetId,
    String actionUrl,
    boolean isRead,
    LocalDateTime readAt,
    LocalDateTime createdAt) {}
