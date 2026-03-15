package com.coffiness.calfit.domain.interview.event;

import com.coffiness.calfit.support.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;

public record InterviewCreatedEvent(
    String tenantId,
    Long interviewScheduleId,
    Long recruitmentId,
    Long actorUserId,
    Long meetingRoomId,
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    List<Long> interviewerUserIds,
    List<Long> applicantIds,
    long occurredAt)
    implements DomainEvent {

  public static InterviewCreatedEvent of(
      String tenantId,
      Long interviewScheduleId,
      Long recruitmentId,
      Long actorUserId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      List<Long> interviewerUserIds,
      List<Long> applicantIds) {
    return new InterviewCreatedEvent(
        tenantId,
        interviewScheduleId,
        recruitmentId,
        actorUserId,
        meetingRoomId,
        scheduledAt,
        durationMinutes,
        interviewerUserIds == null ? List.of() : List.copyOf(interviewerUserIds),
        applicantIds == null ? List.of() : List.copyOf(applicantIds),
        System.currentTimeMillis());
  }
}
