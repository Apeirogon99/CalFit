package com.coffiness.calfit.domain.notification;

import com.coffiness.calfit.core.enums.NotificationType;
import java.util.EnumSet;

public enum NotificationCategory {
  ANNOUNCEMENT(EnumSet.of(NotificationType.ANNOUNCEMENT_CREATED)),
  APPLICATION(EnumSet.of(NotificationType.APPLICATION_PROCESS_CHANGED)),
  MEETING_ROOM(EnumSet.of(NotificationType.MEETING_ROOM_RESERVED)),
  INTERVIEW(
      EnumSet.of(
          NotificationType.INTERVIEW_REQUESTED,
          NotificationType.INTERVIEW_UPDATED,
          NotificationType.INTERVIEW_CANCELLED)),
  SCHEDULE(
      EnumSet.of(
          NotificationType.SCHEDULE_INVITED,
          NotificationType.SCHEDULE_UPDATED,
          NotificationType.SCHEDULE_CANCELLED));

  private final EnumSet<NotificationType> types;

  NotificationCategory(EnumSet<NotificationType> types) {
    this.types = types;
  }

  public EnumSet<NotificationType> types() {
    return types.clone();
  }
}
