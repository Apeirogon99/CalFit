package com.coffiness.calfit.domain.interview;

import com.coffiness.calfit.core.enums.InterviewRound;
import com.coffiness.calfit.core.enums.InterviewStatus;
import java.time.LocalDateTime;
import java.util.List;

public record Interview(
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

  public static Interview confirmed(
      Long id,
      Long recruitmentId,
      Long recruitmentStageId,
      InterviewRound round,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds) {
    return new Interview(
        id,
        recruitmentId,
        recruitmentStageId,
        round,
        meetingRoomId,
        scheduledAt,
        durationMinutes,
        memo,
        InterviewStatus.CONFIRMED,
        interviewerIds,
        applicantIds);
  }
}
