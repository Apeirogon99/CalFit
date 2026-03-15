package com.coffiness.calfit.domain.interview;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewAvailability(
    List<MeetingRoomBusySlot> meetingRoomBusySlots,
    List<InterviewerBusySlot> interviewerBusySlots,
    List<ApplicantBusySlot> applicantBusySlots) {

  public record MeetingRoomBusySlot(
      Long meetingRoomId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  public record InterviewerBusySlot(
      Long interviewerId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}

  public record ApplicantBusySlot(
      Long applicantId, Long interviewScheduleId, LocalDateTime start, LocalDateTime end) {}
}
