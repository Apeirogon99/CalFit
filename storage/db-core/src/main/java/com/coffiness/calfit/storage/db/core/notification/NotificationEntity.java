package com.coffiness.calfit.storage.db.core.notification;

import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "notifications",
    indexes = {
      @Index(
          name = "idx_notifications_recipient_created_at",
          columnList = "tenant_id, recipient_user_id, created_at"),
      @Index(
          name = "idx_notifications_recipient_is_read",
          columnList = "tenant_id, recipient_user_id, is_read")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("status = 'ACTIVE'")
public class NotificationEntity extends TenantBaseEntity {

  @Column(name = "recipient_user_id", nullable = false)
  private Long recipientUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 50)
  private NotificationType type;

  @Column(name = "title", nullable = false, length = 120)
  private String title;

  @Column(name = "content", nullable = false, length = 1000)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 50)
  private NotificationTargetType targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "action_url", length = 255)
  private String actionUrl;

  @Column(name = "is_read", nullable = false)
  private boolean isRead;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Builder
  public NotificationEntity(
      String tenantId,
      Long recipientUserId,
      NotificationType type,
      String title,
      String content,
      NotificationTargetType targetType,
      Long targetId,
      String actionUrl,
      boolean isRead,
      LocalDateTime readAt) {
    super(tenantId);
    this.recipientUserId = recipientUserId;
    this.type = type;
    this.title = title;
    this.content = content;
    this.targetType = targetType;
    this.targetId = targetId;
    this.actionUrl = actionUrl;
    this.isRead = isRead;
    this.readAt = readAt;
  }

  public void markAsRead(LocalDateTime readAt) {
    this.isRead = true;
    this.readAt = readAt;
  }
}
