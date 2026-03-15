package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;

public record NotificationCreateCommand(
    Long recipientUserId,
    NotificationType type,
    String title,
    String content,
    NotificationTargetType targetType,
    Long targetId,
    String actionUrl) {}
