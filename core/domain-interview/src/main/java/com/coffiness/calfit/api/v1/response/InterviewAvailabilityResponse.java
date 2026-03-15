package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.interview.InterviewAvailability;
import java.util.List;

public record InterviewAvailabilityResponse(
    List<MeetingRoomBusySlotResponse> meetingRoomBusySlots,
    List<InterviewerBusySlotResponse> interviewerBusySlots,
    List<ApplicantBusySlotResponse> applicantBusySlots) {

  public static InterviewAvailabilityResponse from(InterviewAvailability availability) {
    return new InterviewAvailabilityResponse(
        availability.meetingRoomBusySlots().stream()
            .map(MeetingRoomBusySlotResponse::from)
            .toList(),
        availability.interviewerBusySlots().stream()
            .map(InterviewerBusySlotResponse::from)
            .toList(),
        availability.applicantBusySlots().stream().map(ApplicantBusySlotResponse::from).toList());
  }
}
