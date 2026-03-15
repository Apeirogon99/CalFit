package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.InterviewAvailability;
import java.time.LocalDateTime;

public record InterviewerBusySlotResponse(
    Long interviewerId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {

  public static InterviewerBusySlotResponse from(InterviewAvailability.InterviewerBusySlot slot) {
    return new InterviewerBusySlotResponse(
        slot.interviewerId(), slot.interviewScheduleId(), slot.start(), slot.end());
  }
}
