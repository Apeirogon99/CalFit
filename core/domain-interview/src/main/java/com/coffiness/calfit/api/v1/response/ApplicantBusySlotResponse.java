package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.InterviewAvailability;
import java.time.LocalDateTime;

public record ApplicantBusySlotResponse(
    Long applicantId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {

  public static ApplicantBusySlotResponse from(InterviewAvailability.ApplicantBusySlot slot) {
    return new ApplicantBusySlotResponse(
        slot.applicantId(), slot.interviewScheduleId(), slot.start(), slot.end());
  }
}
