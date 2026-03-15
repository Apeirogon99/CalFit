package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.InterviewAvailability;
import java.time.LocalDateTime;

public record MeetingRoomBusySlotResponse(
    Long meetingRoomId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {

  public static MeetingRoomBusySlotResponse from(InterviewAvailability.MeetingRoomBusySlot slot) {
    return new MeetingRoomBusySlotResponse(
        slot.meetingRoomId(), slot.interviewScheduleId(), slot.start(), slot.end());
  }
}
