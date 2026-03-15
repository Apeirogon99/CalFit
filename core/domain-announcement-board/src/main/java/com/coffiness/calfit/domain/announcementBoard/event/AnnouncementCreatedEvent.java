package com.coffiness.calfit.domain.announcementBoard.event;

import com.coffiness.calfit.support.event.DomainEvent;

public record AnnouncementCreatedEvent(
    String tenantId, Long announcementId, String title, Long actorUserId, long occurredAt)
    implements DomainEvent {

  public static AnnouncementCreatedEvent of(
      String tenantId, Long announcementId, String title, Long actorUserId) {
    return new AnnouncementCreatedEvent(
        tenantId, announcementId, title, actorUserId, System.currentTimeMillis());
  }
}
