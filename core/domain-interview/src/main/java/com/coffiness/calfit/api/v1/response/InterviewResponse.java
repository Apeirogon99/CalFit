package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.domain.interview.Interview;
import java.time.LocalDateTime;
import java.util.List;

public record InterviewResponse(
    Long id,
    Long recruitmentId,
    Long recruitmentStageId,
    InterviewRound round,
    Long meetingRoomId,
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    String memo,
    InterviewStatus status,
    List<Long> interviewerIds,
    List<Long> applicantIds) {

  public static InterviewResponse from(Interview interview) {
    return new InterviewResponse(
        interview.id(),
        interview.recruitmentId(),
        interview.recruitmentStageId(),
        interview.round(),
        interview.meetingRoomId(),
        interview.scheduledAt(),
        interview.durationMinutes(),
        interview.memo(),
        interview.status(),
        interview.interviewerIds(),
        interview.applicantIds());
  }
}
